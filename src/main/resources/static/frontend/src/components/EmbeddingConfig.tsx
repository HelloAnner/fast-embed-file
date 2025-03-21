import { DownloadOutlined, UploadOutlined } from '@ant-design/icons';
import { Button, Form, Input, InputNumber, Progress, Select, Upload, message } from 'antd';
import axios from 'axios';
import React, { useEffect, useState } from 'react';

const { Option } = Select;

// 添加模型类型列表
const MODEL_TYPES = [
    { value: 'doubao-embedding-text-240715', label: 'doubao-embedding-text-240715' }
];

interface EmbeddingConfigProps { }

const EmbeddingConfig: React.FC<EmbeddingConfigProps> = () => {
    const [form] = Form.useForm();
    const [uploading, setUploading] = useState(false);
    const [progress, setProgress] = useState(0);
    const [processing, setProcessing] = useState(false);
    const [completed, setCompleted] = useState(false);

    useEffect(() => {
        let timer: NodeJS.Timeout;
        if (processing) {
            timer = setInterval(async () => {
                try {
                    const response = await axios.get('/api/embedding/progress');
                    setProgress(response.data.progress);
                    if (response.data.progress >= 100) {
                        setProcessing(false);
                        setCompleted(true);
                        message.success('向量化处理完成！');
                        clearInterval(timer);
                    }
                } catch (error) {
                    console.error('获取进度失败:', error);
                }
            }, 1000);
        }
        return () => timer && clearInterval(timer);
    }, [processing]);

    const onFinish = async (values: any) => {
        try {
            setProcessing(true);
            setCompleted(false);
            const formData = new FormData();
            formData.append('file', values.file[0].originFileObj);
            formData.append('modelType', values.modelType);
            formData.append('baseUrl', values.baseUrl);
            formData.append('apiKey', values.apiKey);
            formData.append('maxTokensPerChunk', values.maxTokensPerChunk.toString());
            formData.append('overlapTokens', values.overlapTokens.toString());

            await axios.post('/api/embedding/process', formData, {
                headers: {
                    'Content-Type': 'multipart/form-data',
                },
            });
        } catch (error) {
            console.error('处理失败:', error);
            message.error('处理失败，请重试');
            setProcessing(false);
            setCompleted(false);
        }
    };

    const handleDownload = async () => {
        try {
            const response = await axios.get('/api/embedding/download', {
                responseType: 'blob'
            });
            const url = window.URL.createObjectURL(new Blob([response.data]));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', 'vector-data.json');
            document.body.appendChild(link);
            link.click();
            link.remove();
            window.URL.revokeObjectURL(url);
        } catch (error) {
            console.error('下载失败:', error);
            message.error('下载失败，请重试');
        }
    };

    const normFile = (e: any) => {
        if (Array.isArray(e)) {
            return e;
        }
        return e?.fileList;
    };

    return (
        <div style={{ maxWidth: 600, margin: '0 auto', padding: 24 }}>
            <h1>文档向量化配置</h1>
            <Form
                form={form}
                layout="vertical"
                onFinish={onFinish}
                initialValues={{
                    maxTokensPerChunk: 1000,
                    overlapTokens: 10,
                    modelType: 'doubao-embedding-text-240715',
                    baseUrl: 'https://ark.cn-beijing.volces.com/api/v3'
                }}
            >
                <Form.Item
                    name="modelType"
                    label="向量化模型类型"
                    rules={[{ required: true, message: '请选择模型类型' }]}
                    tooltip="默认使用 doubao-embedding-text-240715"
                >
                    <Select placeholder="请选择模型类型">
                        {MODEL_TYPES.map(model => (
                            <Option key={model.value} value={model.value}>
                                {model.label}
                            </Option>
                        ))}
                    </Select>
                </Form.Item>

                <Form.Item
                    name="baseUrl"
                    label="豆包 API 地址"
                    rules={[{ required: true, message: '请输入豆包API地址' }]}
                    tooltip="默认使用 https://ark.cn-beijing.volces.com/api/v3"
                >
                    <Input placeholder="请输入豆包API地址" />
                </Form.Item>

                <Form.Item
                    name="apiKey"
                    label="豆包 API Key"
                    rules={[{ required: true, message: '请输入API Key' }]}
                >
                    <Input.Password placeholder="请输入豆包API Key" />
                </Form.Item>

                <Form.Item
                    name="file"
                    label="上传文件"
                    valuePropName="fileList"
                    getValueFromEvent={normFile}
                    extra="支持上传JSON和Markdown文件的压缩包"
                    rules={[{ required: true, message: '请上传文件' }]}
                >
                    <Upload
                        beforeUpload={() => false}
                        accept=".zip,.rar,.7z"
                    >
                        <Button icon={<UploadOutlined />}>选择文件</Button>
                    </Upload>
                </Form.Item>

                <Form.Item
                    name="maxTokensPerChunk"
                    label="最大分块Token数 (MAX_TOKENS_PER_CHUNK)"
                    rules={[{ required: true, message: '请输入最大分块Token数' }]}
                >
                    <InputNumber min={1} style={{ width: '100%' }} />
                </Form.Item>

                <Form.Item
                    name="overlapTokens"
                    label="重叠Token数 (OVER_LAP_TOKENS)"
                    rules={[{ required: true, message: '请输入重叠Token数' }]}
                >
                    <InputNumber min={0} style={{ width: '100%' }} />
                </Form.Item>

                <Form.Item>
                    <Button
                        type="primary"
                        htmlType="submit"
                        loading={processing}
                        style={{ width: '100%' }}
                    >
                        开始向量化
                    </Button>
                </Form.Item>

                {processing && (
                    <Form.Item>
                        <Progress percent={progress} status="active" />
                    </Form.Item>
                )}

                {completed && (
                    <Form.Item>
                        <Button
                            type="primary"
                            onClick={handleDownload}
                            style={{ width: '100%' }}
                            icon={<DownloadOutlined />}
                        >
                            下载向量化文件
                        </Button>
                    </Form.Item>
                )}
            </Form>
        </div>
    );
};

export default EmbeddingConfig; 