import { ExclamationCircleOutlined } from '@ant-design/icons';
import { Button, Card, Modal, Space, Table, Tag, Typography, message } from 'antd';
import axios from 'axios';
import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';

const { Title } = Typography;
const { confirm } = Modal;

interface Task {
    id: string;
    fileName: string;
    modelType: string;
    progress: number;
    status: 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELLED';
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
        const interval = setInterval(fetchTasks, 5000); // 每5秒刷新一次
        return () => clearInterval(interval);
    }, []);

    const handleCancel = (taskId: string) => {
        confirm({
            title: '确认取消任务',
            icon: <ExclamationCircleOutlined />,
            content: '确定要取消这个任务吗？取消后无法恢复。',
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

    const getStatusTag = (status: Task['status']) => {
        const statusConfig = {
            RUNNING: { color: 'processing', text: '运行中' },
            COMPLETED: { color: 'success', text: '已完成' },
            FAILED: { color: 'error', text: '失败' },
            CANCELLED: { color: 'default', text: '已取消' },
        };
        const config = statusConfig[status];
        return <Tag color={config.color}>{config.text}</Tag>;
    };

    const columns = [
        {
            title: '文件名',
            dataIndex: 'fileName',
            key: 'fileName',
        },
        {
            title: '模型类型',
            dataIndex: 'modelType',
            key: 'modelType',
        },
        {
            title: '状态',
            dataIndex: 'status',
            key: 'status',
            render: (status: Task['status']) => getStatusTag(status),
        },
        {
            title: '进度',
            dataIndex: 'progress',
            key: 'progress',
            render: (progress: number) => `${Math.floor(progress)}%`,
        },
        {
            title: '创建时间',
            dataIndex: 'createTime',
            key: 'createTime',
            render: (time: string) => new Date(time).toLocaleString(),
        },
        {
            title: '更新时间',
            dataIndex: 'updateTime',
            key: 'updateTime',
            render: (time: string) => new Date(time).toLocaleString(),
        },
        {
            title: '操作',
            key: 'action',
            render: (_: any, record: Task) => (
                <Space>
                    {record.status === 'RUNNING' && (
                        <Button type="primary" danger onClick={() => handleCancel(record.id)}>
                            取消
                        </Button>
                    )}
                    {record.status === 'FAILED' && (
                        <Button type="link" onClick={() => message.error(record.errorMessage)}>
                            查看错误
                        </Button>
                    )}
                </Space>
            ),
        },
    ];

    return (
        <div style={{ padding: '24px' }}>
            <Card>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '24px' }}>
                    <Title level={3} style={{ margin: 0 }}>任务管理</Title>
                    <Button type="primary" onClick={() => navigate('/')}>
                        返回主页
                    </Button>
                </div>
                <Table
                    columns={columns}
                    dataSource={tasks}
                    rowKey="id"
                    loading={loading}
                    pagination={false}
                />
            </Card>
        </div>
    );
};

export default TaskManager; 