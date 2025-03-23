import { DownloadOutlined, FileTextOutlined, SettingOutlined, UploadOutlined } from '@ant-design/icons';
import { Button, Card, Form, Input, InputNumber, message, Progress, Select, Space, Typography, Upload } from 'antd';
import axios from 'axios';
import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';

const { Option } = Select;
const { Title } = Typography;

// 添加模型类型列表
const MODEL_TYPES = [
    { value: 'doubao-embedding-text-240715', label: '豆包 doubao-embedding-text-240715' },
    { value: 'text-embedding-v1', label: '通义千问 text-embedding-v1' }
];

// 添加API地址列表
const API_URLS = [
    { value: 'https://ark.cn-beijing.volces.com/api/v3', label: 'https://ark.cn-beijing.volces.com/api/v3' },
    { value: 'https://dashscope.aliyuncs.com/compatible-mode/v1', label: 'https://dashscope.aliyuncs.com/compatible-mode/v1' }
];

// 错误码映射
const ERROR_MESSAGES: { [key: string]: string } = {
    '1001': '文件上传失败，请重试',
    '1002': '文件不存在，请重新上传',
    '1003': '文件大小超过限制，请压缩后重试',
    '1004': '无效的文件格式，请上传正确的压缩文件',
    '1005': '文件解压失败，请检查文件完整性',
    '2001': '服务器目录创建失败，请联系管理员',
    '2002': '目录不存在，请联系管理员',
    '2003': '目录访问被拒绝，请联系管理员',
    '3001': '文件处理失败，请重试',
    '3002': '文件读取错误，请重试',
    '3003': '文件写入错误，请重试',
    '4001': '向量化处理失败，请重试',
    '4002': '模型处理错误，请重试',
    '4003': 'API调用错误，请检查API配置',
    '4004': '无效的API密钥，请检查API Key',
    '5001': '系统内部错误，请联系管理员',
    '5002': '未预期的错误，请联系管理员',
    '6001': '无效的参数，请检查输入',
    '6002': '缺少必要参数，请完整填写表单',
    '6003': '参数超出范围，请调整输入值'
};

interface EmbeddingConfigProps { }

const EmbeddingConfig: React.FC<EmbeddingConfigProps> = () => {
    const [form] = Form.useForm();
    const [processing, setProcessing] = useState(false);
    const [completed, setCompleted] = useState(false);
    const [progress, setProgress] = useState(0);
    const [currentTaskId, setCurrentTaskId] = useState<string | null>(null);
    const navigate = useNavigate();

    useEffect(() => {
        let intervalId: ReturnType<typeof setInterval>;
        if (processing && currentTaskId && currentTaskId !== 'COMPLETED') {
            intervalId = setInterval(async () => {
                try {
                    const response = await axios.get(`/api/tasks/${currentTaskId}`);
                    if (response.data.success) {
                        const task = response.data.data;
                        // 确保进度是一个有效的数字
                        const currentProgress = typeof task.progress === 'number' ? task.progress : 0;
                        setProgress(currentProgress);

                        if (task.status === 'COMPLETED') {
                            setProcessing(false);
                            setCompleted(true);
                            setProgress(100);
                            clearInterval(intervalId);
                            message.success('向量化处理完成！');
                            form.resetFields(['file']);
                        } else if (task.status === 'FAILED') {
                            setProcessing(false);
                            setProgress(0);
                            clearInterval(intervalId);
                            message.error(task.errorMessage || '处理失败');
                        } else if (task.status === 'CANCELLED') {
                            setProcessing(false);
                            setProgress(0);
                            clearInterval(intervalId);
                            message.info('任务已取消');
                        }
                    } else {
                        throw new Error(response.data.message || '获取进度失败');
                    }
                } catch (error) {
                    console.error('获取进度失败:', error);
                    handleError(error);
                    setProcessing(false);
                    setProgress(0);
                    clearInterval(intervalId);
                }
            }, 1000);
        }
        return () => {
            if (intervalId) {
                clearInterval(intervalId);
            }
        };
    }, [processing, currentTaskId, form]);

    const handleError = (error: any) => {
        if (error.response?.data) {
            const { code, message: errorMessage } = error.response.data;
            if (!error.response.data.success) {
                message.error(ERROR_MESSAGES[code] || errorMessage || '处理失败，请重试');
            }
        } else {
            message.error('网络错误，请检查网络连接');
        }
    };

    const handleCancel = async () => {
        if (currentTaskId) {
            try {
                const response = await axios.post(`/api/tasks/${currentTaskId}/cancel`);
                if (response.data.success) {
                    message.success('任务已取消');
                    setProcessing(false);
                    setCurrentTaskId(null);
                }
            } catch (error) {
                console.error('取消任务失败:', error);
                message.error('取消任务失败');
            }
        }
    };

    const onFinish = async (values: any) => {
        try {
            setProcessing(true);
            setCompleted(false);
            setProgress(0); // 重置进度
            const formData = new FormData();
            formData.append('file', values.file[0].originFileObj);
            formData.append('modelType', values.modelType);
            formData.append('baseUrl', values.baseUrl);
            formData.append('apiKey', values.apiKey);
            formData.append('maxTokensPerChunk', values.maxTokensPerChunk.toString());
            formData.append('overlapTokens', values.overlapTokens.toString());

            const response = await axios.post('/api/embedding/process', formData, {
                headers: {
                    'Content-Type': 'multipart/form-data',
                },
            });

            if (response.data.success) {
                const taskId = response.data.data?.taskId;
                if (taskId === 'COMPLETED') {
                    // 同步完成的情况
                    setProcessing(false);
                    setCompleted(true);
                    setProgress(100);
                    message.success('向量化处理完成！');
                    form.resetFields(['file']);
                } else if (taskId) {
                    // 异步处理的情况
                    setCurrentTaskId(taskId);
                    setProgress(0); // 开始异步处理时重置进度
                } else {
                    throw new Error('无效的任务ID');
                }
            } else {
                throw new Error(response.data.message || '处理失败');
            }
        } catch (error) {
            console.error('处理失败:', error);
            handleError(error);
            setProcessing(false);
            setCompleted(false);
            setProgress(0);
        }
    };

    const handleDownload = async () => {
        try {
            if (!currentTaskId) {
                message.error('无法下载文件：任务ID不存在');
                return;
            }
            // 创建一个隐藏的 a 标签来下载文件
            const link = document.createElement('a');
            link.href = `/api/embedding/download/${currentTaskId}`;
            link.target = '_blank'; // 在新标签页中打开
            link.rel = 'noopener noreferrer'; // 安全性设置
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
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
        <div style={{
            maxWidth: 800,
            margin: '40px auto',
            padding: '0 24px',
            minHeight: '100vh',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center'
        }}>
            <div style={{
                textAlign: 'center',
                marginBottom: 40,
                width: '100%',
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center'
            }}>
                <div style={{ flex: 1 }}></div>
                <div style={{ textAlign: 'center' }}>
                    <FileTextOutlined style={{ fontSize: 48, color: '#1890ff', marginBottom: 16 }} />
                    <Title level={2} style={{ margin: 0 }}>文档向量化工具</Title>
                    <Typography.Text type="secondary">
                        支持处理 JSON 和 Markdown 格式的压缩文件
                    </Typography.Text>
                </div>
                <div style={{ flex: 1, textAlign: 'right' }}>
                    <Button
                        type="link"
                        icon={<SettingOutlined />}
                        onClick={() => navigate('/tasks')}
                    >
                        任务管理
                    </Button>
                </div>
            </div>

            <Card style={{ width: '100%', borderRadius: 8 }} bordered={false}>
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
                    >
                        <Select placeholder="请选择模型类型" size="large">
                            {MODEL_TYPES.map(model => (
                                <Option key={model.value} value={model.value}>
                                    {model.label}
                                </Option>
                            ))}
                        </Select>
                    </Form.Item>

                    <Form.Item
                        name="baseUrl"
                        label="API 地址"
                        rules={[{ required: true, message: '请选择API地址' }]}
                    >
                        <Select placeholder="请选择API地址" size="large">
                            {API_URLS.map(url => (
                                <Option key={url.value} value={url.value}>
                                    {url.label}
                                </Option>
                            ))}
                        </Select>
                    </Form.Item>

                    <Form.Item
                        name="apiKey"
                        label="API Key"
                        rules={[{ required: true, message: '请输入API Key' }]}
                    >
                        <Input.Password placeholder="请输入API Key" size="large" />
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
                            maxCount={1}
                            beforeUpload={() => false}
                            accept=".zip,.rar,.7z"
                        >
                            <Button icon={<UploadOutlined />} size="large">选择文件</Button>
                        </Upload>
                    </Form.Item>

                    <div style={{
                        display: 'grid',
                        gridTemplateColumns: '1fr 1fr',
                        gap: '16px',
                        marginBottom: '24px'
                    }}>
                        <Form.Item
                            name="maxTokensPerChunk"
                            label="最大分块Token数"
                            rules={[{ required: true, message: '请输入最大分块Token数' }]}
                        >
                            <InputNumber min={1} style={{ width: '100%' }} size="large" />
                        </Form.Item>

                        <Form.Item
                            name="overlapTokens"
                            label="重叠Token数"
                            rules={[{ required: true, message: '请输入重叠Token数' }]}
                        >
                            <InputNumber min={0} style={{ width: '100%' }} size="large" />
                        </Form.Item>
                    </div>

                    <Form.Item style={{ marginBottom: processing || completed ? 24 : 0 }}>
                        <Space style={{ width: '100%', justifyContent: 'center' }}>
                            <Button
                                type="primary"
                                htmlType="submit"
                                loading={processing}
                                disabled={completed}
                                size="large"
                                style={{ 
                                    width: processing ? '200px' : '100%',
                                    height: '48px',
                                    fontSize: '16px'
                                }}
                            >
                                开始向量化
                            </Button>
                            {processing && (
                                <Button
                                    danger
                                    size="large"
                                    onClick={handleCancel}
                                    style={{
                                        width: '120px',
                                        height: '48px',
                                        fontSize: '16px'
                                    }}
                                >
                                    取消任务
                                </Button>
                            )}
                        </Space>
                    </Form.Item>

                    {processing && (
                        <Form.Item>
                            <Progress
                                percent={Math.floor(progress)}
                                status="active"
                                strokeColor={{
                                    '0%': '#108ee9',
                                    '100%': '#87d068',
                                }}
                            />
                        </Form.Item>
                    )}

                    {completed && (
                        <Form.Item>
                            <Button
                                type="primary"
                                icon={<DownloadOutlined />}
                                onClick={handleDownload}
                                size="large"
                                style={{
                                    width: '100%',
                                    height: '48px',
                                    fontSize: '16px',
                                    background: '#52c41a'
                                }}
                            >
                                下载向量化结果
                            </Button>
                        </Form.Item>
                    )}
                </Form>
            </Card>
        </div>
    );
};

export default EmbeddingConfig; 