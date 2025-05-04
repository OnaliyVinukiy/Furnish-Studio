import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import HomePage from "./pages/HomePage";

import RoomDesignPage from "./pages/DesignPage";
import ThreeDView from "./pages/ThreeDView";

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/design" element={<RoomDesignPage />} />
        <Route path="/3d-view" element={<ThreeDView />} />
      </Routes>
    </Router>
  );
}

export default App;
