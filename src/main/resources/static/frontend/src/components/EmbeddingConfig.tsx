import { CheckCircleFilled, DownloadOutlined, FileTextOutlined, LoadingOutlined, SettingOutlined, UploadOutlined } from '@ant-design/icons';
import { Button, Card, Form, Input, InputNumber, message, Modal, Progress, Select, Space, Typography, Upload } from 'antd';
import axios from 'axios';
import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';

const { Option } = Select;
const { Title } = Typography;

// 添加模型类型列表
const MODEL_TYPES = [
    {
        value: 'doubao-embedding-text-240715',
        label: '豆包 doubao-embedding-text-240715',
        description: '请确保已在豆包平台开通该模型的使用权限',
        warning: '使用前请确保：\n1. 已在豆包平台开通该模型的使用权限\n2. API Key 具有足够的调用额度\n3. API 地址配置正确'
    },
    {
        value: 'text-embedding-v1',
        label: '通义千问 text-embedding-v1',
        description: '请确保已在通义千问平台开通该模型的使用权限',
        warning: '使用前请确保：\n1. 已在通义千问平台开通该模型的使用权限\n2. API Key 具有足够的调用额度\n3. API 地址配置正确'
    }
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

interface StageInfo {
    key: string;
    title: string;
    progress: number;
    completed: boolean;
}

const EmbeddingConfig: React.FC<EmbeddingConfigProps> = () => {
    const [form] = Form.useForm();
    const [processing, setProcessing] = useState(false);
    const [completed, setCompleted] = useState(false);
    const [progress, setProgress] = useState(0);
    const [currentTaskId, setCurrentTaskId] = useState<string | null>(null);
    const [configTested, setConfigTested] = useState(false);
    const [testing, setTesting] = useState(false);
    const [stages, setStages] = useState<StageInfo[]>([
        { key: 'prepare', title: '准备处理文件', progress: 0, completed: false },
        { key: 'segment', title: '文本分段', progress: 0, completed: false },
        { key: 'embedding', title: '生成文本向量', progress: 0, completed: false },
        { key: 'complete', title: '处理完成', progress: 0, completed: false },
    ]);
    const navigate = useNavigate();

    useEffect(() => {
        let intervalId: ReturnType<typeof setInterval>;
        if (processing && currentTaskId && currentTaskId !== 'COMPLETED') {
            intervalId = setInterval(async () => {
                try {
                    const response = await axios.get(`/api/tasks/${currentTaskId}`);
                    if (response.data.success) {
                        const task = response.data.data;
                        setProgress(task.progress);

                        // 更新各阶段状态
                        const newStages = [...stages];

                        if (task.currentStage === "准备处理文件") {
                            newStages[0].progress = 100;
                            newStages[0].completed = true;
                        } else if (task.currentStage === "正在分析文件并进行文本分段") {
                            newStages[0].completed = true;
                            newStages[0].progress = 100;
                            newStages[1].progress = task.segmentProgress;
                        } else if (task.currentStage === "正在生成文本向量") {
                            newStages[0].completed = true;
                            newStages[0].progress = 100;
                            newStages[1].completed = true;
                            newStages[1].progress = 100;
                            newStages[2].progress = task.embeddingProgress;
                        } else if (task.currentStage === "处理完成") {
                            newStages.forEach(stage => {
                                stage.completed = true;
                                stage.progress = 100;
                            });
                        }

                        setStages(newStages);

                        if (task.status === 'COMPLETED') {
                            setProcessing(false);
                            setCompleted(true);
                            setProgress(100);
                            clearInterval(intervalId);
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
    }, [processing, currentTaskId, form, stages]);

    const handleError = (error: any) => {
        if (error.response?.data) {
            const { code, message: errorMessage, detail } = error.response.data;
            if (!error.response.data.success) {
                // 使用 Modal 显示详细错误信息
                Modal.error({
                    title: errorMessage || '处理失败',
                    content: (
                        <div style={{ whiteSpace: 'pre-line' }}>
                            {detail || ERROR_MESSAGES[code] || '处理失败，请重试'}
                        </div>
                    ),
                    width: 500
                });
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

    const handleTestConfig = async (values: any) => {
        try {
            setTesting(true);
            const formData = new FormData();
            formData.append('file', values.file[0].originFileObj);
            formData.append('modelType', values.modelType);
            formData.append('baseUrl', values.baseUrl);
            formData.append('apiKey', values.apiKey);

            const response = await axios.post('/api/embedding/test', formData, {
                headers: {
                    'Content-Type': 'multipart/form-data',
                },
            });

            if (response.data.success) {
                message.success('配置测试成功');
                setConfigTested(true);
            } else {
                throw new Error(response.data.message || '配置测试失败');
            }
        } catch (error) {
            console.error('配置测试失败:', error);
            handleError(error);
        } finally {
            setTesting(false);
        }
    };

    const onFinish = async (values: any) => {
        if (!configTested) {
            await handleTestConfig(values);
            return;
        }

        try {
            setProcessing(true);
            setCompleted(false);
            setProgress(0);
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
                setCurrentTaskId(response.data.data.taskId);
                message.success('任务已提交');
            } else {
                throw new Error(response.data.message || '提交失败');
            }
        } catch (error) {
            console.error('提交失败:', error);
            handleError(error);
            setProcessing(false);
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
                        label="向量化模型"
                        name="modelType"
                        rules={[{ required: true, message: '请选择向量化模型' }]}
                    >
                        <Select
                            placeholder="请选择向量化模型"
                            listHeight={200}
                            optionLabelProp="label"
                        >
                            {MODEL_TYPES.map(model => (
                                <Option
                                    key={model.value}
                                    value={model.value}
                                    label={model.label}
                                    title={model.description}
                                >
                                    <div style={{ padding: '8px 0' }}>
                                        <div style={{
                                            fontWeight: 500,
                                            marginBottom: '4px',
                                            lineHeight: '20px'
                                        }}>{model.label}</div>
                                        <div style={{
                                            fontSize: '12px',
                                            color: '#999',
                                            lineHeight: '16px'
                                        }}>{model.description}</div>
                                    </div>
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
                        extra="上传原始文件的压缩包"
                        rules={[{ required: true, message: '请上传文件' }]}
                    >
                        <Upload
                            maxCount={1}
                            beforeUpload={() => false}
                            accept=".zip,.rar,.7z"
                            onChange={(info) => {
                                if (info.fileList.length > 0) {
                                    const modelType = form.getFieldValue('modelType');
                                    const model = MODEL_TYPES.find(m => m.value === modelType);
                                    if (model) {
                                        message.warning({
                                            content: model.warning,
                                            duration: 3,
                                            key: 'modelWarning'
                                        });
                                    }
                                }
                            }}
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

                    <Form.Item>
                        <Space style={{ width: '100%', justifyContent: 'center' }}>
                            {!completed ? (
                                <>
                                    {!configTested ? (
                                        <Button 
                                            type="primary" 
                                            htmlType="submit" 
                                            loading={testing}
                                            disabled={processing}
                                            size="large"
                                            style={{ 
                                                width: processing ? '200px' : '100%',
                                                height: '48px',
                                                fontSize: '16px'
                                            }}
                                        >
                                            测试配置
                                        </Button>
                                    ) : (
                                        <Button 
                                            type="primary" 
                                            htmlType="submit" 
                                            loading={processing}
                                            disabled={!configTested}
                                            size="large"
                                            style={{ 
                                                width: processing ? '200px' : '100%',
                                                height: '48px',
                                                fontSize: '16px'
                                            }}
                                        >
                                            开始向量化
                                        </Button>
                                    )}
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
                                </>
                            ) : (
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
                            )}
                        </Space>
                    </Form.Item>

                    {processing && (
                        <Form.Item style={{ width: '100%' }}>
                            <div style={{
                                display: 'flex',
                                flexDirection: 'column',
                                gap: '12px',
                                background: '#f5f5f5',
                                padding: '16px',
                                borderRadius: '8px'
                            }}>
                                {stages.map((stage, index) => (
                                    <div key={stage.key} style={{
                                        display: 'flex',
                                        alignItems: 'center',
                                        gap: '12px'
                                    }}>
                                        {stage.completed ? (
                                            <CheckCircleFilled style={{ color: '#52c41a', fontSize: '20px' }} />
                                        ) : stage.progress > 0 ? (
                                            <LoadingOutlined style={{ color: '#1890ff', fontSize: '20px' }} />
                                        ) : (
                                            <div style={{ width: '20px', height: '20px', borderRadius: '50%', border: '1px solid #d9d9d9' }} />
                                        )}
                                        <div style={{ flex: 1 }}>
                                            <div style={{
                                                display: 'flex',
                                                justifyContent: 'space-between',
                                                marginBottom: '4px'
                                            }}>
                                                <span style={{ color: stage.completed ? '#52c41a' : stage.progress > 0 ? '#1890ff' : '#666' }}>
                                                    {stage.title}
                                                </span>
                                                {stage.progress > 0 && !stage.completed && (
                                                    <span style={{ color: '#1890ff' }}>
                                                        {Math.floor(stage.progress)}%
                                                    </span>
                                                )}
                                            </div>
                                            {stage.progress > 0 && !stage.completed && (
                                                <Progress
                                                    percent={stage.progress}
                                                    size="small"
                                                    showInfo={false}
                                                    strokeColor="#1890ff"
                                                />
                                            )}
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </Form.Item>
                    )}
                </Form>
            </Card>
        </div>
    );
};

export default EmbeddingConfig; 