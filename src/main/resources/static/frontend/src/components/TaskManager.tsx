import { CheckCircleFilled, CloseCircleFilled, DownloadOutlined, HomeOutlined, LoadingOutlined, StopOutlined } from '@ant-design/icons';
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
            width: '25%',
            ellipsis: true,
        },
        {
            title: '模型类型',
            dataIndex: 'modelType',
            key: 'modelType',
            width: '15%',
            ellipsis: true,
        },
        {
            title: '状态',
            dataIndex: 'status',
            key: 'status',
            width: '10%',
            render: (status: string) => getStatusTag(status),
        },
        {
            title: '当前阶段',
            dataIndex: 'currentStage',
            key: 'currentStage',
            width: '20%',
            ellipsis: true,
        },
        {
            title: '时间',
            key: 'time',
            width: '20%',
            render: (_, record: Task) => (
                <div style={{ fontSize: '12px' }}>
                    <div>创建: {record.createTime}</div>
                    <div>更新: {record.updateTime}</div>
                </div>
            ),
        },
        {
            title: '操作',
            key: 'action',
            width: '10%',
            render: (_: any, record: Task) => (
                <Space size="small" direction="vertical" style={{ width: '100%' }}>
                    {record.status === 'RUNNING' && (
                        <Button
                            danger
                            type="primary"
                            icon={<StopOutlined />}
                            onClick={() => handleCancel(record.id)}
                            size="small"
                            style={{ width: '100%' }}
                        >
                            中断
                        </Button>
                    )}
                    {record.status === 'COMPLETED' && (
                        <Button
                            type="primary"
                            icon={<DownloadOutlined />}
                            onClick={() => handleDownload(record.id)}
                            size="small"
                            style={{ width: '100%', background: '#52c41a', borderColor: '#52c41a' }}
                        >
                            下载
                        </Button>
                    )}
                </Space>
            ),
        },
    ];

    const tableStyles = {
        runningRow: {
            backgroundColor: '#f0f9ff'
        },
        tableRow: {
            '&:hover': {
                boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
                transition: 'all 0.3s'
            }
        }
    };

    return (
        <div style={{ maxWidth: '100%', margin: '20px auto', padding: '0 16px' }}>
            <Card bordered={false} style={{ borderRadius: '8px', boxShadow: '0 2px 8px rgba(0,0,0,0.1)' }}>
                <div style={{ 
                    marginBottom: 24, 
                    display: 'flex', 
                    justifyContent: 'space-between', 
                    alignItems: 'center' 
                }}>
                    <div style={{ flex: 1 }}>
                        <Title level={3} style={{ margin: 0, color: '#1890ff', textAlign: 'center' }}>任务管理</Title>
                    </div>
                    <Button 
                        type="primary" 
                        icon={<HomeOutlined />}
                        onClick={() => navigate('/')}
                        style={{ 
                            background: '#52c41a', 
                            borderColor: '#52c41a',
                            position: 'absolute',
                            right: 24,
                            top: 24
                        }}
                    >
                        返回主页
                    </Button>
                </div>
                <Table
                    columns={columns}
                    dataSource={tasks}
                    rowKey="id"
                    loading={loading}
                    pagination={{
                        pageSize: 10,
                        showTotal: (total) => `共 ${total} 条记录`,
                        showSizeChanger: true,
                        showQuickJumper: true,
                        size: 'small'
                    }}
                    size="small"
                    style={{ marginTop: 16 }}
                    onRow={(record) => ({
                        style: {
                            ...(record.status === 'RUNNING' ? tableStyles.runningRow : {}),
                            ...tableStyles.tableRow
                        }
                    })}
                />
            </Card>
        </div>
    );
};

export default TaskManager; 