import React from 'react';
import { Route, BrowserRouter as Router, Routes } from 'react-router-dom';
import EmbeddingConfig from './components/EmbeddingConfig';
import TaskManager from './components/TaskManager';

const App: React.FC = () => {
    return (
        <Router>
            <Routes>
                <Route path="/" element={<EmbeddingConfig />} />
                <Route path="/tasks" element={<TaskManager />} />
            </Routes>
        </Router>
    );
};

export default App; 