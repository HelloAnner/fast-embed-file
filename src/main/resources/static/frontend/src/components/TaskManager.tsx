import { CheckCircleFilled, CloseCircleFilled, DownloadOutlined, LoadingOutlined, StopOutlined } from '@ant-design/icons';
import { Button, Card, message, Modal, Space, Table, Tag, Typography } from 'antd';
import axios from 'axios';
import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';

const { Title } = Typography;

interface Task {
    id: string;
    fileName: string;
    modelType: string;
    progress: number;
    status: 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELLED';
    currentStage: string;
    createTime: string;
    updateTime: string;
    errorMessage?: string;
}

const TaskManager: React.FC = () => {
    const [tasks, setTasks] = useState<Task[]>([]);
    const [loading, setLoading] = useState(false);
    const navigate = useNavigate();

    const fetchTasks = async () => {
        try {
            const response = await axios.get('/api/tasks');
            if (response.data.success) {
                setTasks(response.data.data);
            }
        } catch (error) {
            console.error('获取任务列表失败:', error);
            message.error('获取任务列表失败');
        }
    };

    useEffect(() => {
        fetchTasks();
        const intervalId = setInterval(fetchTasks, 5000);
        return () => clearInterval(intervalId);
    }, []);

    const handleCancel = async (taskId: string) => {
        Modal.confirm({
            title: '确认取消任务',
            content: '确定要取消这个任务吗？',
            okText: '确定',
            okType: 'danger',
            cancelText: '取消',
            onOk: async () => {
                try {
                    const response = await axios.post(`/api/tasks/${taskId}/cancel`);
                    if (response.data.success) {
                        message.success('任务已取消');
                        fetchTasks();
                    }
                } catch (error) {
                    console.error('取消任务失败:', error);
                    message.error('取消任务失败');
                }
            },
        });
    };

    const handleDownload = async (taskId: string) => {
        try {
            const link = document.createElement('a');
            link.href = `/api/embedding/download/${taskId}`;
            link.target = '_blank';
            link.rel = 'noopener noreferrer';
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
        } catch (error) {
            console.error('下载失败:', error);
            message.error('下载失败，请重试');
        }
    };

    const getStatusIcon = (status: string) => {
        switch (status) {
            case 'COMPLETED':
                return <CheckCircleFilled style={{ color: '#52c41a' }} />;
            case 'FAILED':
                return <CloseCircleFilled style={{ color: '#ff4d4f' }} />;
            case 'CANCELLED':
                return <StopOutlined style={{ color: '#faad14' }} />;
            case 'RUNNING':
                return <LoadingOutlined style={{ color: '#1890ff' }} />;
            default:
                return null;
        }
    };

    const getStatusTag = (status: string) => {
        let color = '';
        let text = '';
        switch (status) {
            case 'COMPLETED':
                color = 'success';
                text = '已完成';
                break;
            case 'FAILED':
                color = 'error';
                text = '失败';
                break;
            case 'CANCELLED':
                color = 'warning';
                text = '已取消';
                break;
            case 'RUNNING':
                color = 'processing';
                text = '处理中';
                break;
            default:
                color = 'default';
                text = '未知';
        }
        return (
            <Tag icon={getStatusIcon(status)} color={color}>
                {text}
            </Tag>
        );
    };

    const columns = [
        {
            title: '文件名',
            dataIndex: 'fileName',
            key: 'fileName',
            width: 200,
        },
        {
            title: '模型类型',
            dataIndex: 'modelType',
            key: 'modelType',
            width: 200,
        },
        {
            title: '状态',
            dataIndex: 'status',
            key: 'status',
            width: 120,
            render: (status: string) => getStatusTag(status),
        },
        {
            title: '当前阶段',
            dataIndex: 'currentStage',
            key: 'currentStage',
            width: 200,
        },
        {
            title: '创建时间',
            dataIndex: 'createTime',
            key: 'createTime',
            width: 180,
        },
        {
            title: '更新时间',
            dataIndex: 'updateTime',
            key: 'updateTime',
            width: 180,
        },
        {
            title: '操作',
            key: 'action',
            width: 200,
            render: (_: any, record: Task) => (
                <Space>
                    {record.status === 'RUNNING' && (
                        <Button
                            danger
                            type="primary"
                            icon={<StopOutlined />}
                            onClick={() => handleCancel(record.id)}
                        >
                            中断
                        </Button>
                    )}
                    {record.status === 'COMPLETED' && (
                        <Button
                            type="primary"
                            icon={<DownloadOutlined />}
                            onClick={() => handleDownload(record.id)}
                            style={{ background: '#52c41a', borderColor: '#52c41a' }}
                        >
                            下载
                        </Button>
                    )}
                </Space>
            ),
        },
    ];

    return (
        <div style={{ maxWidth: 1200, margin: '40px auto', padding: '0 24px' }}>
            <Card bordered={false}>
                <div style={{ marginBottom: 24, textAlign: 'center' }}>
                    <Title level={2}>任务管理</Title>
                </div>
                <Table
                    columns={columns}
                    dataSource={tasks}
                    rowKey="id"
                    loading={loading}
                    pagination={false}
                    scroll={{ x: 1200 }}
                />
            </Card>
        </div>
    );
};

export default TaskManager; 