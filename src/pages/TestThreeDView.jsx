import React, { useEffect, useRef, useState } from "react";
import { useLocation, Link } from "react-router-dom";
import * as THREE from "three";
import { OrbitControls } from "three/examples/jsm/controls/OrbitControls.js";
import { GLTFLoader } from "three/examples/jsm/loaders/GLTFLoader.js";
import { DragControls } from "three/examples/jsm/controls/DragControls.js";

function TestThreeDView() {
  const mountRef = useRef(null);
  const location = useLocation();
  const { room, furniture } = location.state || {
    room: {
      dimensions: { width: 20, length: 20 },
      wallColor: "#f5f5f5",
      floorColor: "#e0e0e0",
      floorType: "tile",
      floorTexture: "",
    },
    furniture: [],
  };

  // State management
  const [furnitureColors, setFurnitureColors] = useState(() => {
    const initialColors = {};
    furniture.forEach((item) => {
      initialColors[item.id] = item.color;
    });
    return initialColors;
  });

  const [selectedFurnitureId, setSelectedFurnitureId] = useState(null);
  const [furnitureScales, setFurnitureScales] = useState(() => {
    const initialScales = {};
    furniture.forEach((item) => {
      initialScales[item.id] = 1;
    });
    return initialScales;
  });

  const [furnitureRotations, setFurnitureRotations] = useState(() => {
    const initialRotations = {};
    furniture.forEach((item) => {
      initialRotations[item.id] = { x: 0, y: 0, z: 0 };
    });
    return initialRotations;
  });

  const [furniturePositions, setFurniturePositions] = useState(() => {
    const initialPositions = {};
    furniture.forEach((item) => {
      initialPositions[item.id] = {
        x: item.x,
        y: item.y,
        z: 0,
      };
    });
    return initialPositions;
  });

  // Undo/redo functionality
  const [history, setHistory] = useState([]);
  const [historyIndex, setHistoryIndex] = useState(-1);

  const furnitureModelsRef = useRef({});
  const rendererRef = useRef(null);
  const sceneRef = useRef(null);
  const cameraRef = useRef(null);
  const controlsRef = useRef(null);
  const dragControlsRef = useRef(null);
  const isMountedRef = useRef(false);
  const raycasterRef = useRef(new THREE.Raycaster());
  const mouseRef = useRef(new THREE.Vector2());

  const FIXED_WALL_HEIGHT = 5;

  // Save state to history
  const saveStateToHistory = () => {
    const currentState = {
      positions: { ...furniturePositions },
      rotations: { ...furnitureRotations },
      scales: { ...furnitureScales },
      colors: { ...furnitureColors },
    };

    // If we're not at the end of history, discard future states
    const newHistory = history.slice(0, historyIndex + 1);
    setHistory([...newHistory, currentState]);
    setHistoryIndex(newHistory.length);
  };

  // Undo action
  const undo = () => {
    if (historyIndex <= 0) return;
    const prevState = history[historyIndex - 1];
    applyState(prevState);
    setHistoryIndex(historyIndex - 1);
  };

  // Redo action
  const redo = () => {
    if (historyIndex >= history.length - 1) return;
    const nextState = history[historyIndex + 1];
    applyState(nextState);
    setHistoryIndex(historyIndex + 1);
  };

  // Apply state from history
  const applyState = (state) => {
    setFurniturePositions(state.positions);
    setFurnitureRotations(state.rotations);
    setFurnitureScales(state.scales);
    setFurnitureColors(state.colors);

    // Apply to models
    Object.entries(furnitureModelsRef.current).forEach(([id, model]) => {
      if (model) {
        const pos = state.positions[id];
        const rot = state.rotations[id];
        const scale = state.scales[id];
        const color = state.colors[id];

        if (pos) {
          model.position.set(pos.x, pos.y, pos.z);
        }

        if (rot) {
          model.rotation.set(rot.x, rot.y, rot.z);
        }

        if (scale) {
          const baseScale = model.userData.baseScale || 1;
          model.scale.set(
            baseScale * scale,
            baseScale * scale,
            baseScale * scale
          );
        }

        if (color) {
          applyColorToModel(model, color);
        }
      }
    });
  };

  // Apply color to a model
  const applyColorToModel = (model, color) => {
    if (!model) return;
    model.traverse((child) => {
      if (child.isMesh && child.material) {
        child.material.color.set(color);
        child.material.needsUpdate = true;
      }
    });
  };

  // Update furniture colors when furniture prop changes
  useEffect(() => {
    setFurnitureColors((prev) => {
      const updatedColors = { ...prev };
      furniture.forEach((item) => {
        updatedColors[item.id] = item.color;
        const model = furnitureModelsRef.current[item.id];
        applyColorToModel(model, item.color);
      });
      return updatedColors;
    });
  }, [furniture]);

  // Handle scaling
  const handleScaleChange = (furnitureId, newScale) => {
    setFurnitureScales((prev) => ({
      ...prev,
      [furnitureId]: newScale,
    }));

    const model = furnitureModelsRef.current[furnitureId];
    if (model) {
      const item = furniture.find((f) => f.id === furnitureId);
      const baseScale = model.userData.baseScale || 1;
      model.scale.set(
        baseScale * newScale,
        baseScale * newScale,
        baseScale * newScale
      );

      const boundingBox = new THREE.Box3().setFromObject(model);
      const minY = boundingBox.min.y;
      model.position.y = -minY + ((item.models && item.models[0].yOffset) || 0);

      // Update position in state
      setFurniturePositions((prev) => ({
        ...prev,
        [furnitureId]: {
          x: model.position.x,
          y: model.position.y,
          z: model.position.z,
        },
      }));
    }

    saveStateToHistory();
  };

  // Handle rotation - updated to rotate around current position
  const handleRotationChange = (furnitureId, axis, newRotation) => {
    const model = furnitureModelsRef.current[furnitureId];
    if (!model) return;

    // Save current position before rotation
    const currentPosition = model.position.clone();

    // Update rotation in state
    setFurnitureRotations((prev) => ({
      ...prev,
      [furnitureId]: {
        ...prev[furnitureId],
        [axis]: newRotation,
      },
    }));

    // Apply the rotation
    model.rotation[axis] = newRotation;

    // After rotation, ensure the model stays in the same position
    model.position.copy(currentPosition);

    saveStateToHistory();
  };

  // Handle position change
  const handlePositionChange = (furnitureId, axis, value) => {
    setFurniturePositions((prev) => {
      const newPositions = {
        ...prev,
        [furnitureId]: {
          ...prev[furnitureId],
          [axis]: value,
        },
      };

      const model = furnitureModelsRef.current[furnitureId];
      if (model) {
        model.position[axis] = value;
      }

      return newPositions;
    });

    saveStateToHistory();
  };

  // Handle mouse click for selection
  const handleMouseClick = (event) => {
    event.preventDefault();
    mouseRef.current.x = (event.clientX / window.innerWidth) * 2 - 1;
    mouseRef.current.y = -(event.clientY / window.innerHeight) * 2 + 1;

    raycasterRef.current.setFromCamera(mouseRef.current, cameraRef.current);
    const intersects = raycasterRef.current.intersectObjects(
      Object.values(furnitureModelsRef.current),
      true
    );

    if (intersects.length > 0) {
      let selectedModel = intersects[0].object;
      while (selectedModel && !selectedModel.userData.furnitureId) {
        selectedModel = selectedModel.parent;
      }
      if (selectedModel && selectedModel.userData.furnitureId) {
        setSelectedFurnitureId(selectedModel.userData.furnitureId);
      } else {
        setSelectedFurnitureId(null);
      }
    } else {
      setSelectedFurnitureId(null);
    }
  };

  useEffect(() => {
    if (!mountRef.current || isMountedRef.current) return;

    isMountedRef.current = true;

    // Initialize scene, camera, and renderer
    const scene = new THREE.Scene();
    scene.background = new THREE.Color(0xffffff);
    sceneRef.current = scene;

    const camera = new THREE.PerspectiveCamera(
      50,
      window.innerWidth / window.innerHeight,
      0.1,
      1000
    );
    cameraRef.current = camera;

    const renderer = new THREE.WebGLRenderer({
      antialias: true,
      powerPreference: "high-performance",
    });
    renderer.setPixelRatio(window.devicePixelRatio);
    renderer.setSize(window.innerWidth, window.innerHeight);
    renderer.shadowMap.enabled = true;
    renderer.shadowMap.type = THREE.PCFSoftShadowMap;
    renderer.toneMapping = THREE.ACESFilmicToneMapping;
    renderer.toneMappingExposure = 1.2;
    rendererRef.current = renderer;

    // Clear existing canvases and append new one
    while (mountRef.current.firstChild) {
      mountRef.current.removeChild(mountRef.current.firstChild);
    }
    mountRef.current.appendChild(renderer.domElement);

    const roomWidth = room.dimensions.width;
    const roomLength = room.dimensions.length;
    const roomHeight = FIXED_WALL_HEIGHT;

    // Create room geometry
    const roomGroup = new THREE.Group();
    const textureLoader = new THREE.TextureLoader();

    // Floor with dynamic material
    const floorGeometry = new THREE.PlaneGeometry(roomWidth, roomLength);
    let floorMaterial;

    if (room.floorTexture) {
      textureLoader.load(
        room.floorTexture,
        (texture) => {
          texture.wrapS = texture.wrapT = THREE.RepeatWrapping;
          const repeatFactor =
            room.floorType === "tile" ? 4 : room.floorType === "carpet" ? 2 : 3;
          texture.repeat.set(
            roomWidth / repeatFactor,
            roomLength / repeatFactor
          );
          floorMaterial = new THREE.MeshStandardMaterial({
            map: texture,
            color: room.floorColor,
            roughness:
              room.floorType === "carpet"
                ? 0.9
                : room.floorType === "tile"
                ? 0.3
                : 0.5,
            metalness: room.floorType === "tile" ? 0.2 : 0,
            side: THREE.DoubleSide,
          });
          const floor = new THREE.Mesh(floorGeometry, floorMaterial);
          floor.rotation.x = -Math.PI / 2;
          floor.receiveShadow = true;
          roomGroup.add(floor);
        },
        undefined,
        (error) => {
          console.error(
            `Error loading floor texture ${room.floorTexture}:`,
            error
          );
          floorMaterial = new THREE.MeshStandardMaterial({
            color: room.floorColor,
            roughness:
              room.floorType === "carpet"
                ? 0.9
                : room.floorType === "tile"
                ? 0.3
                : 0.5,
            metalness: room.floorType === "tile" ? 0.2 : 0,
            side: THREE.DoubleSide,
          });
          const floor = new THREE.Mesh(floorGeometry, floorMaterial);
          floor.rotation.x = -Math.PI / 2;
          floor.receiveShadow = true;
          roomGroup.add(floor);
        }
      );
    } else {
      floorMaterial = new THREE.MeshStandardMaterial({
        color: room.floorColor,
        roughness:
          room.floorType === "carpet"
            ? 0.9
            : room.floorType === "tile"
            ? 0.3
            : 0.5,
        metalness: room.floorType === "tile" ? 0.2 : 0,
        side: THREE.DoubleSide,
      });
      const floor = new THREE.Mesh(floorGeometry, floorMaterial);
      floor.rotation.x = -Math.PI / 2;
      floor.receiveShadow = true;
      roomGroup.add(floor);
    }

    // Ceiling
    const ceilingMaterial = new THREE.MeshStandardMaterial({
      color: "#f8f8f8",
      roughness: 0.6,
      metalness: 0,
    });
    const ceiling = new THREE.Mesh(
      new THREE.PlaneGeometry(roomWidth, roomLength),
      ceilingMaterial
    );
    ceiling.position.set(0, roomHeight, 0);
    ceiling.rotation.x = Math.PI / 2;
    ceiling.receiveShadow = true;
    roomGroup.add(ceiling);

    // Walls
    const wallMaterial = new THREE.MeshStandardMaterial({
      color: room.wallColor,
      roughness: 0.5,
      metalness: 0,
      side: THREE.DoubleSide,
    });

    const frontWallGeometry = new THREE.PlaneGeometry(roomWidth, roomHeight);
    const frontWall = new THREE.Mesh(frontWallGeometry, wallMaterial);
    frontWall.position.set(0, roomHeight / 2, -roomLength / 2);
    frontWall.receiveShadow = true;
    roomGroup.add(frontWall);

    const backWallGeometry = new THREE.PlaneGeometry(roomWidth, roomHeight);
    const backWall = new THREE.Mesh(backWallGeometry, wallMaterial);
    backWall.position.set(0, roomHeight / 2, roomLength / 2);
    backWall.rotation.y = Math.PI;
    backWall.receiveShadow = true;
    roomGroup.add(backWall);

    const sideWallGeometry = new THREE.PlaneGeometry(roomLength, roomHeight);
    const leftWall = new THREE.Mesh(sideWallGeometry, wallMaterial);
    leftWall.position.set(-roomWidth / 2, roomHeight / 2, 0);
    leftWall.rotation.y = Math.PI / 2;
    leftWall.receiveShadow = true;
    roomGroup.add(leftWall);

    const rightWall = new THREE.Mesh(sideWallGeometry, wallMaterial);
    rightWall.position.set(roomWidth / 2, roomHeight / 2, 0);
    rightWall.rotation.y = -Math.PI / 2;
    rightWall.receiveShadow = true;
    roomGroup.add(rightWall);

    // Baseboard/trim
    const trimGeometry = new THREE.BoxGeometry(roomWidth + 0.2, 0.2, 0.1);
    const trimMaterial = new THREE.MeshStandardMaterial({
      color: "#333333",
      roughness: 0.4,
      metalness: 0.2,
    });

    const backTrim = new THREE.Mesh(trimGeometry, trimMaterial);
    backTrim.position.set(0, 0.1, roomLength / 2 - 0.05);
    roomGroup.add(backTrim);

    const sideTrimGeometry = new THREE.BoxGeometry(roomLength + 0.2, 0.2, 0.1);
    const leftTrim = new THREE.Mesh(sideTrimGeometry, trimMaterial);
    leftTrim.position.set(-roomWidth / 2 + 0.05, 0.1, 0);
    leftTrim.rotation.y = Math.PI / 2;
    roomGroup.add(leftTrim);

    const rightTrim = new THREE.Mesh(sideTrimGeometry, trimMaterial);
    rightTrim.position.set(roomWidth / 2 - 0.05, 0.1, 0);
    rightTrim.rotation.y = Math.PI / 2;
    roomGroup.add(rightTrim);

    scene.add(roomGroup);

    // Add lighting
    const ambientLight = new THREE.AmbientLight(0xffffff, 0.3);
    scene.add(ambientLight);

    const ceilingLight = new THREE.RectAreaLight(
      0xffffff,
      5,
      roomWidth * 0.7,
      roomLength * 0.7
    );
    ceilingLight.position.set(0, roomHeight - 0.1, 0);
    ceilingLight.rotation.x = Math.PI / 2;
    scene.add(ceilingLight);

    const maxDimension = Math.max(roomWidth, roomLength);
    const sunLight = new THREE.DirectionalLight(0xfff4e6, 1.5);
    sunLight.position.set(0, FIXED_WALL_HEIGHT * 1.5, maxDimension * 1.5);
    sunLight.castShadow = true;
    sunLight.shadow.mapSize.width = 2048;
    sunLight.shadow.mapSize.height = 2048;
    sunLight.shadow.camera.near = 0.5;
    sunLight.shadow.camera.far = maxDimension * 3;
    sunLight.shadow.camera.left = -maxDimension * 0.5;
    sunLight.shadow.camera.right = maxDimension * 0.5;
    sunLight.shadow.camera.top = FIXED_WALL_HEIGHT;
    sunLight.shadow.camera.bottom = -FIXED_WALL_HEIGHT;
    sunLight.shadow.bias = -0.001;
    scene.add(sunLight);

    const fillLight1 = new THREE.DirectionalLight(0xffffff, 0.4);
    fillLight1.position.set(-roomWidth, FIXED_WALL_HEIGHT / 2, 0);
    scene.add(fillLight1);

    const fillLight2 = new THREE.DirectionalLight(0xffffff, 0.4);
    fillLight2.position.set(roomWidth, FIXED_WALL_HEIGHT / 2, 0);
    scene.add(fillLight2);

    // Load furniture models
    const gltfLoader = new GLTFLoader();
    const draggableObjects = [];

    furniture.forEach((item) => {
      gltfLoader.load(
        item.modelPath,
        (gltf) => {
          const model = gltf.scene;
          const boundingBox = new THREE.Box3().setFromObject(model);
          const modelSize = boundingBox.getSize(new THREE.Vector3());
          const baseScale = Math.min(
            item.width / (modelSize.x || 1),
            item.length / (modelSize.z || 1),
            FIXED_WALL_HEIGHT / (modelSize.y || 1)
          );
          model.scale.set(baseScale, baseScale, baseScale);
          model.userData = { furnitureId: item.id, baseScale };

          const scaledBoundingBox = new THREE.Box3().setFromObject(model);
          const minY = scaledBoundingBox.min.y;
          model.position.set(
            item.x - roomWidth / 2 + item.width / 2,
            -minY + ((item.models && item.models[0].yOffset) || 0),
            item.y - roomLength / 2 + item.length / 2
          );

          model.traverse((child) => {
            if (child.isMesh) {
              child.castShadow = true;
              child.receiveShadow = true;
              if (child.material) {
                child.material.color.set(
                  furnitureColors[item.id] || item.color
                );
                child.material.roughness = 0.3;
                child.material.metalness = 0.2;
                child.material.needsUpdate = true;
              }
            }
          });

          scene.add(model);
          furnitureModelsRef.current[item.id] = model;
          draggableObjects.push(model);

          // Apply initial scale and rotation
          const scale = furnitureScales[item.id] || 1;
          model.scale.set(
            baseScale * scale,
            baseScale * scale,
            baseScale * scale
          );

          const rotation = furnitureRotations[item.id] || { x: 0, y: 0, z: 0 };
          model.rotation.set(rotation.x, rotation.y, rotation.z);

          // Save initial position
          setFurniturePositions((prev) => ({
            ...prev,
            [item.id]: {
              x: model.position.x,
              y: model.position.y,
              z: model.position.z,
            },
          }));
        },
        undefined,
        (error) => {
          console.error(`Error loading model ${item.modelPath}:`, error);
          const geometry = new THREE.BoxGeometry(item.width, 2, item.length);
          const material = new THREE.MeshStandardMaterial({
            color: furnitureColors[item.id] || item.color,
            roughness: 0.3,
            metalness: 0.2,
          });
          const cube = new THREE.Mesh(geometry, material);
          cube.position.set(
            item.x - roomWidth / 2 + item.width / 2,
            1,
            item.y - roomLength / 2 + item.length / 2
          );
          cube.userData = { furnitureId: item.id, baseScale: 1 };
          cube.castShadow = true;
          cube.receiveShadow = true;
          scene.add(cube);
          furnitureModelsRef.current[item.id] = cube;
          draggableObjects.push(cube);

          // Apply initial scale and rotation
          const scale = furnitureScales[item.id] || 1;
          cube.scale.set(scale, scale, scale);

          const rotation = furnitureRotations[item.id] || { x: 0, y: 0, z: 0 };
          cube.rotation.set(rotation.x, rotation.y, rotation.z);

          // Save initial position
          setFurniturePositions((prev) => ({
            ...prev,
            [item.id]: {
              x: cube.position.x,
              y: cube.position.y,
              z: cube.position.z,
            },
          }));
        }
      );
    });

    // Camera and controls
    const zDistance = FIXED_WALL_HEIGHT * 3 + maxDimension * 0.5;
    camera.position.set(0, FIXED_WALL_HEIGHT * 0.7, zDistance);
    camera.lookAt(0, FIXED_WALL_HEIGHT * 0.3, 0);

    const controls = new OrbitControls(camera, renderer.domElement);
    controls.target.set(0, FIXED_WALL_HEIGHT * 0.3, 0);
    controls.minDistance = FIXED_WALL_HEIGHT * 1;
    controls.maxDistance = FIXED_WALL_HEIGHT * 10;
    controls.maxPolarAngle = Math.PI * 0.9;
    controls.enablePan = true;
    controls.enableDamping = true;
    controls.dampingFactor = 0.05;
    controls.update();
    controlsRef.current = controls;

    // Drag controls
    const dragControls = new DragControls(
      draggableObjects,
      camera,
      renderer.domElement
    );
    dragControlsRef.current = dragControls;

    dragControls.addEventListener("dragstart", () => {
      controls.enabled = false;
    });

    dragControls.addEventListener("dragend", () => {
      controls.enabled = true;
      saveStateToHistory();
    });

    dragControls.addEventListener("drag", (event) => {
      const model = event.object;
      const item = furniture.find((f) => f.id === model.userData.furnitureId);
      if (!item) return;

      // Constrain position within room boundaries
      const boundingBox = new THREE.Box3().setFromObject(model);
      const size = boundingBox.getSize(new THREE.Vector3());
      const halfWidth = (item.width * (furnitureScales[item.id] || 1)) / 2;
      const halfLength = (item.length * (furnitureScales[item.id] || 1)) / 2;

      model.position.x = Math.max(
        -roomWidth / 2 + halfWidth,
        Math.min(roomWidth / 2 - halfWidth, model.position.x)
      );
      model.position.z = Math.max(
        -roomLength / 2 + halfLength,
        Math.min(roomLength / 2 - halfLength, model.position.z)
      );

      // Ensure model stays on the floor
      const minY = boundingBox.min.y;
      model.position.y = -minY + ((item.models && item.models[0].yOffset) || 0);

      // Update position in state
      setFurniturePositions((prev) => ({
        ...prev,
        [item.id]: {
          x: model.position.x,
          y: model.position.y,
          z: model.position.z,
        },
      }));
    });

    // Add click listener for selection
    renderer.domElement.addEventListener("click", handleMouseClick);

    // Animation loop
    let animationFrameId;
    const animate = () => {
      animationFrameId = requestAnimationFrame(animate);
      controls.update();
      renderer.render(scene, camera);
    };
    animate();

    // Handle window resize
    const handleResize = () => {
      camera.aspect = window.innerWidth / window.innerHeight;
      camera.updateProjectionMatrix();
      renderer.setSize(window.innerWidth, window.innerHeight);
    };
    window.addEventListener("resize", handleResize);

    // Save initial state to history
    saveStateToHistory();

    // Cleanup
    return () => {
      isMountedRef.current = false;

      if (animationFrameId) {
        cancelAnimationFrame(animationFrameId);
      }

      window.removeEventListener("resize", handleResize);
      renderer.domElement.removeEventListener("click", handleMouseClick);

      if (mountRef.current) {
        while (mountRef.current.firstChild) {
          mountRef.current.removeChild(mountRef.current.firstChild);
        }
      }

      if (rendererRef.current) {
        rendererRef.current.dispose();
        rendererRef.current.forceContextLoss();
        rendererRef.current = null;
      }

      if (sceneRef.current) {
        sceneRef.current.traverse((object) => {
          if (object.geometry) object.geometry.dispose();
          if (object.material) {
            if (Array.isArray(object.material)) {
              object.material.forEach((mat) => mat.dispose());
            } else {
              object.material.dispose();
            }
          }
        });
        sceneRef.current.clear();
        sceneRef.current = null;
      }

      cameraRef.current = null;
      controlsRef.current = null;
      dragControlsRef.current = null;
      furnitureModelsRef.current = {};
    };
  }, [
    room.dimensions.width,
    room.dimensions.length,
    room.wallColor,
    room.floorColor,
    room.floorType,
    room.floorTexture,
  ]);

  const handleColorChange = (furnitureId, newColor) => {
    setFurnitureColors((prev) => ({
      ...prev,
      [furnitureId]: newColor,
    }));

    const model = furnitureModelsRef.current[furnitureId];
    applyColorToModel(model, newColor);

    saveStateToHistory();
  };

  return (
    <div className="min-h-screen bg-neutral-50 font-sans">
      <header className="flex justify-between items-center p-8">
        <div className="flex items-center">
          <svg
            className="h-8 w-8 text-indigo-600"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth="2"
              d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"
            />
          </svg>
          <span className="ml-2 text-xl font-bold text-gray-900">
            Furnish Studio
          </span>
        </div>
        <Link
          to="/design"
          className="px-4 py-2 bg-indigo-600 text-white rounded-full hover:bg-indigo-700 transition-colors duration-200"
        >
          Back to 2D Design
        </Link>
      </header>
      <div className="flex">
        <div ref={mountRef} className="w-3/4 h-[calc(100vh-80px)]" />
        <div className="w-1/4 bg-white p-6 rounded-l-2xl shadow-xl h-[calc(100vh-80px)] overflow-y-auto">
          <div className="flex justify-between items-center mb-6 border-b pb-2">
            <h3 className="text-xl font-semibold text-gray-900">
              Furniture Editor
            </h3>
            <div className="flex space-x-2">
              <button
                onClick={undo}
                disabled={historyIndex <= 0}
                className={`p-2 rounded-md ${
                  historyIndex <= 0
                    ? "bg-gray-200 text-gray-500"
                    : "bg-indigo-100 text-indigo-700 hover:bg-indigo-200"
                }`}
                title="Undo"
              >
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  className="h-5 w-5"
                  viewBox="0 0 20 20"
                  fill="currentColor"
                >
                  <path
                    fillRule="evenodd"
                    d="M7.707 3.293a1 1 0 010 1.414L5.414 7H11a7 7 0 017 7v2a1 1 0 11-2 0v-2a5 5 0 00-5-5H5.414l2.293 2.293a1 1 0 11-1.414 1.414l-4-4a1 1 0 010-1.414l4-4a1 1 0 011.414 0z"
                    clipRule="evenodd"
                  />
                </svg>
              </button>
              <button
                onClick={redo}
                disabled={historyIndex >= history.length - 1}
                className={`p-2 rounded-md ${
                  historyIndex >= history.length - 1
                    ? "bg-gray-200 text-gray-500"
                    : "bg-indigo-100 text-indigo-700 hover:bg-indigo-200"
                }`}
                title="Redo"
              >
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  className="h-5 w-5"
                  viewBox="0 0 20 20"
                  fill="currentColor"
                >
                  <path
                    fillRule="evenodd"
                    d="M12.293 5.293a1 1 0 011.414 0l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414-1.414L14.586 11H3a1 1 0 110-2h11.586l-2.293-2.293a1 1 0 010-1.414z"
                    clipRule="evenodd"
                  />
                </svg>
              </button>
            </div>
          </div>

          {furniture.length === 0 ? (
            <p className="text-sm text-gray-600">No furniture added.</p>
          ) : (
            <div className="space-y-6">
              {furniture.map((item) => (
                <div
                  key={item.id}
                  className={`p-3 rounded-lg border transition-colors duration-150 ${
                    selectedFurnitureId === item.id
                      ? "border-indigo-500 bg-indigo-50"
                      : "border-gray-100 hover:bg-gray-50"
                  }`}
                >
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-sm font-medium text-gray-900">
                      {item.name}
                    </span>
                    <div className="flex items-center">
                      <div
                        className="w-6 h-6 rounded-md mr-2 border border-gray-300"
                        style={{
                          backgroundColor:
                            furnitureColors[item.id] || item.color,
                        }}
                      />
                      <input
                        type="color"
                        value={furnitureColors[item.id] || item.color}
                        onChange={(e) =>
                          handleColorChange(item.id, e.target.value)
                        }
                        className="p-1 w-8 h-8 border rounded-md cursor-pointer"
                      />
                    </div>
                  </div>
                  <div className="text-xs text-gray-500 mb-2">
                    {item.width}m × {item.length}m
                  </div>
                  {selectedFurnitureId === item.id && (
                    <div className="space-y-4">
                      <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">
                          Position
                        </label>
                        <div className="grid grid-cols-3 gap-2 mb-2">
                          <div>
                            <label className="block text-xs text-gray-500 mb-1">
                              X
                            </label>
                            <input
                              type="range"
                              min={-100}
                              max={100}
                              step={0.1}
                              value={furniturePositions[item.id]?.x || 0}
                              onChange={(e) =>
                                handlePositionChange(
                                  item.id,
                                  "x",
                                  parseFloat(e.target.value)
                                )
                              }
                              className="w-full"
                            />
                            <div className="text-sm text-center mt-1">
                              {furniturePositions[item.id]?.x?.toFixed(1) || 0}
                            </div>
                          </div>
                          <div>
                            <label className="block text-xs text-gray-500 mb-1">
                              Y
                            </label>
                            <input
                              type="range"
                              min={-100}
                              max={100}
                              step={0.1}
                              value={furniturePositions[item.id]?.y || 0}
                              onChange={(e) =>
                                handlePositionChange(
                                  item.id,
                                  "y",
                                  parseFloat(e.target.value)
                                )
                              }
                              className="w-full"
                            />
                            <div className="text-sm text-center mt-1">
                              {furniturePositions[item.id]?.y?.toFixed(1) || 0}
                            </div>
                          </div>
                          <div>
                            <label className="block text-xs text-gray-500 mb-1">
                              Z
                            </label>
                            <input
                              type="range"
                              min={-100}
                              max={100}
                              step={0.1}
                              value={furniturePositions[item.id]?.z || 0}
                              onChange={(e) =>
                                handlePositionChange(
                                  item.id,
                                  "z",
                                  parseFloat(e.target.value)
                                )
                              }
                              className="w-full"
                            />
                            <div className="text-sm text-center mt-1">
                              {furniturePositions[item.id]?.z?.toFixed(1) || 0}
                            </div>
                          </div>
                        </div>
                      </div>

                      <div>
                        <label className="block text-sm font-medium text-gray-700">
                          Scale ({furnitureScales[item.id]?.toFixed(2) || 1})
                        </label>
                        <input
                          type="range"
                          min="0.5"
                          max="2"
                          step="0.1"
                          value={furnitureScales[item.id] || 1}
                          onChange={(e) =>
                            handleScaleChange(
                              item.id,
                              parseFloat(e.target.value)
                            )
                          }
                          className="w-full h-2 bg-gray-200 rounded-lg cursor-pointer"
                        />
                      </div>

                      <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">
                          Rotation
                        </label>
                        <div className="grid grid-cols-3 gap-2">
                          <div>
                            <label className="block text-xs text-gray-500 mb-1">
                              X (
                              {(
                                (furnitureRotations[item.id]?.x || 0) *
                                (180 / Math.PI)
                              ).toFixed(0)}
                              °)
                            </label>
                            <input
                              type="range"
                              min="0"
                              max={2 * Math.PI}
                              step={Math.PI / 180}
                              value={furnitureRotations[item.id]?.x || 0}
                              onChange={(e) =>
                                handleRotationChange(
                                  item.id,
                                  "x",
                                  parseFloat(e.target.value)
                                )
                              }
                              className="w-full h-2 bg-gray-200 rounded-lg cursor-pointer"
                            />
                          </div>
                          <div>
                            <label className="block text-xs text-gray-500 mb-1">
                              Y (
                              {(
                                (furnitureRotations[item.id]?.y || 0) *
                                (180 / Math.PI)
                              ).toFixed(0)}
                              °)
                            </label>
                            <input
                              type="range"
                              min="0"
                              max={2 * Math.PI}
                              step={Math.PI / 180}
                              value={furnitureRotations[item.id]?.y || 0}
                              onChange={(e) =>
                                handleRotationChange(
                                  item.id,
                                  "y",
                                  parseFloat(e.target.value)
                                )
                              }
                              className="w-full h-2 bg-gray-200 rounded-lg cursor-pointer"
                            />
                          </div>
                          <div>
                            <label className="block text-xs text-gray-500 mb-1">
                              Z (
                              {(
                                (furnitureRotations[item.id]?.z || 0) *
                                (180 / Math.PI)
                              ).toFixed(0)}
                              °)
                            </label>
                            <input
                              type="range"
                              min="0"
                              max={2 * Math.PI}
                              step={Math.PI / 180}
                              value={furnitureRotations[item.id]?.z || 0}
                              onChange={(e) =>
                                handleRotationChange(
                                  item.id,
                                  "z",
                                  parseFloat(e.target.value)
                                )
                              }
                              className="w-full h-2 bg-gray-200 rounded-lg cursor-pointer"
                            />
                          </div>
                        </div>
                      </div>
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

export default TestThreeDView;
