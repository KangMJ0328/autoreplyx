import { useState, useEffect, useCallback } from 'react';
import { logsAPI, MessageLog } from '../lib/api';

export function useLogs() {
  const [logs, setLogs] = useState<MessageLog[]>([]);
  const [pagination, setPagination] = useState({
    current_page: 1,
    last_page: 1,
    per_page: 20,
    total: 0,
  });
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchLogs = useCallback(async (page: number = 1) => {
    try {
      setIsLoading(true);
      setError(null);

      const response = await logsAPI.list({
        page,
        per_page: 20,
      });

      setLogs(response.data);
      setPagination({
        current_page: response.meta.current_page,
        last_page: response.meta.last_page,
        per_page: response.meta.per_page,
        total: response.meta.total,
      });
    } catch (err: any) {
      setError(err.response?.data?.message || '로그를 불러오는데 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchLogs(1);
  }, []);

  const exportLogs = async (format: string = 'csv') => {
    await logsAPI.export({});
  };

  return {
    logs,
    pagination,
    isLoading,
    error,
    fetchLogs,
    exportLogs,
  };
}
