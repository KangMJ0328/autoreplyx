import { useState, useEffect, useCallback } from 'react';
import { channelsAPI, Channel } from '../lib/api';

export function useChannels() {
  const [channels, setChannels] = useState<Channel[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchChannels = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      const result = await channelsAPI.list();
      setChannels(result);
    } catch (err: any) {
      setError(err.response?.data?.message || '채널 정보를 불러오는데 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchChannels();
  }, [fetchChannels]);

  // Instagram OAuth 연동
  const connectInstagram = async () => {
    try {
      setError(null);
      const response = await channelsAPI.connectInstagram();
      if (response?.auth_url) {
        window.location.href = response.auth_url;
      }
    } catch (err: any) {
      setError(err.response?.data?.error || 'Instagram 연동에 실패했습니다.');
    }
  };

  // 인스타그램 개발 모드 연동 (Mock)
  const mockConnectInstagram = async (username: string) => {
    try {
      setError(null);
      await channelsAPI.mockConnectInstagram(username);
      await fetchChannels();
      return true;
    } catch (err: any) {
      setError(err.response?.data?.error || 'Instagram 연동에 실패했습니다.');
      return false;
    }
  };

  // 카카오톡 OAuth 연동
  const connectKakao = async () => {
    try {
      setError(null);
      const response = await channelsAPI.connectKakao();
      if (response?.auth_url) {
        window.location.href = response.auth_url;
      }
    } catch (err: any) {
      setError(err.response?.data?.error || '카카오톡 연동에 실패했습니다.');
    }
  };

  // 네이버 OAuth 연동
  const connectNaver = async () => {
    try {
      setError(null);
      const response = await channelsAPI.connectNaver();
      if (response?.auth_url) {
        window.location.href = response.auth_url;
      }
    } catch (err: any) {
      setError(err.response?.data?.error || '네이버 톡톡 연동에 실패했습니다.');
    }
  };

  // 카카오톡 개발 모드 연동 (Mock)
  const mockConnectKakao = async (channelId: string) => {
    try {
      setError(null);
      await channelsAPI.mockConnectKakao(channelId);
      await fetchChannels();
      return true;
    } catch (err: any) {
      setError(err.response?.data?.error || '카카오톡 연동에 실패했습니다.');
      return false;
    }
  };

  // 네이버 톡톡 개발 모드 연동 (Mock)
  const mockConnectNaver = async (talktalkId: string) => {
    try {
      setError(null);
      await channelsAPI.mockConnectNaver(talktalkId);
      await fetchChannels();
      return true;
    } catch (err: any) {
      setError(err.response?.data?.error || '네이버 톡톡 연동에 실패했습니다.');
      return false;
    }
  };

  const disconnectChannel = async (channelId: number) => {
    try {
      await channelsAPI.disconnect(channelId.toString());
      await fetchChannels();
    } catch (err: any) {
      setError(err.response?.data?.message || '채널 연동 해제에 실패했습니다.');
    }
  };

  const refreshToken = async (channelId: number) => {
    try {
      await channelsAPI.refreshToken(channelId.toString());
      await fetchChannels();
    } catch (err: any) {
      setError(err.response?.data?.message || '토큰 갱신에 실패했습니다.');
    }
  };

  return {
    channels,
    isLoading,
    error,
    fetchChannels,
    connectInstagram,
    mockConnectInstagram,
    connectKakao,
    mockConnectKakao,
    connectNaver,
    mockConnectNaver,
    disconnectChannel,
    refreshToken,
  };
}
