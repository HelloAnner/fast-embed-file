import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import React from 'react';
import EmbeddingConfig from './components/EmbeddingConfig';

const App: React.FC = () => {
    return (
        <ConfigProvider locale={zhCN}>
            <div className="App">
                <EmbeddingConfig />
            </div>
        </ConfigProvider>
    );
};

export default App; 