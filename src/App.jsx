import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import HomePage from "./pages/HomePage";

import RoomDesignPage from "./pages/DesignPage";
import RoomTestDesignPage from "./pages/TestDesign";
import ThreeDView from "./pages/ThreeDView";
import TestThreeDView from "./pages/TestThreeDView";

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/design" element={<RoomDesignPage />} />
        <Route path="/3d-view" element={<ThreeDView />} />
        <Route path="/testDesign" element={<RoomTestDesignPage />} />
        <Route path="/test3dView" element={<TestThreeDView />} />
      </Routes>
    </Router>
  );
}

export default App;
