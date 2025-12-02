import { useState, useEffect, useCallback } from 'react';
import { dashboardAPI, DashboardStats, MessageLog, ChartDataPoint, ChannelStat, ResponseStat, DashboardSummary } from '../lib/api';

export function useDashboard() {
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [recentActivity, setRecentActivity] = useState<MessageLog[]>([]);
  const [chartData, setChartData] = useState<ChartDataPoint[]>([]);
  const [channelStats, setChannelStats] = useState<ChannelStat[]>([]);
  const [responseStats, setResponseStats] = useState<ResponseStat[]>([]);
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchStats = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      const data = await dashboardAPI.stats();
      setStats(data);
    } catch (err: any) {
      setError(err.response?.data?.message || '통계를 불러오는데 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  }, []);

  const fetchRecentActivity = useCallback(async (limit: number = 10) => {
    try {
      const data = await dashboardAPI.recentActivity(limit);
      setRecentActivity(data);
    } catch (err: any) {
      console.error('Failed to fetch recent activity:', err);
    }
  }, []);

  const fetchChartData = useCallback(async (days: number = 7) => {
    try {
      const data = await dashboardAPI.chart(days);
      setChartData(data);
    } catch (err: any) {
      console.error('Failed to fetch chart data:', err);
    }
  }, []);

  const fetchChannelStats = useCallback(async () => {
    try {
      const data = await dashboardAPI.channelStats();
      setChannelStats(data);
    } catch (err: any) {
      console.error('Failed to fetch channel stats:', err);
    }
  }, []);

  const fetchResponseStats = useCallback(async () => {
    try {
      const data = await dashboardAPI.responseStats();
      setResponseStats(data);
    } catch (err: any) {
      console.error('Failed to fetch response stats:', err);
    }
  }, []);

  const fetchSummary = useCallback(async () => {
    try {
      const data = await dashboardAPI.summary();
      setSummary(data);
    } catch (err: any) {
      console.error('Failed to fetch summary:', err);
    }
  }, []);

  useEffect(() => {
    fetchStats();
    fetchRecentActivity();
    fetchChartData();
    fetchChannelStats();
    fetchResponseStats();
    fetchSummary();
  }, [fetchStats, fetchRecentActivity, fetchChartData, fetchChannelStats, fetchResponseStats, fetchSummary]);

  const refresh = () => {
    fetchStats();
    fetchRecentActivity();
    fetchChartData();
    fetchChannelStats();
    fetchResponseStats();
    fetchSummary();
  };

  return {
    stats,
    recentActivity,
    chartData,
    channelStats,
    responseStats,
    summary,
    isLoading,
    error,
    refresh,
  };
}
