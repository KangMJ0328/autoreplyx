import { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Instagram, MessageCircle, CheckCircle, ExternalLink, Loader2, AlertCircle, RefreshCw, X } from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
import { useChannels } from '../hooks/useChannels';

export default function Channels() {
  const { channels, isLoading, error, connectInstagram, connectKakao, connectNaver, mockConnectInstagram, mockConnectKakao, mockConnectNaver, disconnectChannel, refreshToken, fetchChannels } = useChannels();
  const [searchParams, setSearchParams] = useSearchParams();

  // 모달 상태 (개발 모드용)
  const [showDevModal, setShowDevModal] = useState<string | null>(null);
  const [inputValue, setInputValue] = useState('');
  const [isConnecting, setIsConnecting] = useState(false);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // OAuth 콜백 처리
  useEffect(() => {
    const success = searchParams.get('success');
    const errorParam = searchParams.get('error');

    if (success) {
      const channelName = success === 'instagram' ? 'Instagram' : success === 'kakao' ? '카카오톡' : '네이버 톡톡';
      setSuccessMessage(`${channelName} 연동이 완료되었습니다!`);
      fetchChannels();
      // URL 파라미터 제거
      setSearchParams({});
      setTimeout(() => setSuccessMessage(null), 5000);
    }

    if (errorParam) {
      // error는 useChannels에서 처리
      setSearchParams({});
    }
  }, [searchParams, setSearchParams, fetchChannels]);

  const availableChannels = [
    {
      id: 'instagram',
      name: 'Instagram',
      description: 'DM 자동응답',
      icon: Instagram,
      iconBg: 'bg-gradient-to-br from-purple-500 to-pink-500',
      inputLabel: '인스타그램 사용자명',
      inputPlaceholder: '@username',
    },
    {
      id: 'kakao',
      name: 'KakaoTalk',
      description: '채널 자동응답',
      icon: MessageCircle,
      iconBg: 'bg-yellow-400',
      inputLabel: '카카오톡 채널 ID',
      inputPlaceholder: '채널 ID 입력',
    },
    {
      id: 'naver',
      name: 'Naver Talk Talk',
      description: '톡톡 자동응답',
      icon: MessageCircle,
      iconBg: 'bg-green-500',
      inputLabel: '네이버 톡톡 ID',
      inputPlaceholder: '톡톡 ID 입력',
    },
  ];

  const isChannelConnected = (channelId: string) => {
    return channels.some(c => c.channel_type.toLowerCase() === channelId && c.is_active);
  };

  // OAuth 연동 시작
  const handleConnect = async (channelId: string) => {
    setIsConnecting(true);
    try {
      if (channelId === 'instagram') {
        await connectInstagram();
      } else if (channelId === 'kakao') {
        await connectKakao();
      } else if (channelId === 'naver') {
        await connectNaver();
      }
    } finally {
      setIsConnecting(false);
    }
  };

  // 개발 모드 Mock 연동 (Instagram만)
  const handleDevConnect = async (channelId: string) => {
    setShowDevModal(channelId);
    setInputValue('');
  };

  const handleSubmitDevConnect = async () => {
    if (!inputValue.trim() || !showDevModal) return;

    setIsConnecting(true);
    try {
      let success = false;
      let channelName = '';

      if (showDevModal === 'instagram') {
        success = await mockConnectInstagram(inputValue);
        channelName = 'Instagram';
      } else if (showDevModal === 'kakao') {
        success = await mockConnectKakao(inputValue);
        channelName = '카카오톡';
      } else if (showDevModal === 'naver') {
        success = await mockConnectNaver(inputValue);
        channelName = '네이버 톡톡';
      }

      if (success) {
        setShowDevModal(null);
        setInputValue('');
        setSuccessMessage(`${channelName} 연동이 완료되었습니다! (개발 모드)`);
        setTimeout(() => setSuccessMessage(null), 5000);
      }
    } finally {
      setIsConnecting(false);
    }
  };

  const handleDisconnect = async (channelId: number) => {
    if (confirm('이 채널 연동을 해제하시겠습니까?')) {
      await disconnectChannel(channelId);
    }
  };

  const handleRefreshToken = async (channelId: number) => {
    await refreshToken(channelId);
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('ko-KR');
  };

  if (isLoading) {
    return (
      <div className="p-8 flex items-center justify-center h-full">
        <div className="text-center">
          <Loader2 className="w-8 h-8 text-blue-500 animate-spin mx-auto mb-4" />
          <p className="text-gray-600">채널 정보를 불러오는 중...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="p-8">
      {/* Success Message */}
      {successMessage && (
        <div className="mb-6 p-4 bg-green-50 border border-green-200 rounded-lg text-green-600">
          <div className="flex items-center">
            <CheckCircle className="w-5 h-5 mr-2" />
            {successMessage}
          </div>
        </div>
      )}

      {/* Error Message */}
      {error && (
        <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg text-red-600">
          <div className="flex items-center">
            <AlertCircle className="w-5 h-5 mr-2" />
            {error}
          </div>
        </div>
      )}

      {/* Header */}
      <div className="mb-8">
        <h1 className="text-gray-900 mb-2">채널 연동</h1>
        <p className="text-gray-600">메시징 채널을 연결하여 자동응답을 시작하세요</p>
      </div>

      {/* Available Channels */}
      <div className="mb-8">
        <h2 className="text-gray-900 mb-4">연동 가능 채널</h2>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {availableChannels.map((channel) => {
            const Icon = channel.icon;
            const connected = isChannelConnected(channel.id);
            return (
              <Card key={channel.id}>
                <CardHeader>
                  <div className="flex items-start justify-between mb-4">
                    <div className={`w-12 h-12 rounded-lg ${channel.iconBg} flex items-center justify-center`}>
                      <Icon className="w-6 h-6 text-white" />
                    </div>
                    {connected && (
                      <Badge className="bg-green-50 text-green-700 border-green-200">
                        <CheckCircle className="w-3 h-3 mr-1" />
                        연동됨
                      </Badge>
                    )}
                  </div>
                  <CardTitle>{channel.name}</CardTitle>
                  <CardDescription>{channel.description}</CardDescription>
                </CardHeader>
                <CardContent>
                  {connected ? (
                    <Button variant="outline" className="w-full">
                      설정 관리
                    </Button>
                  ) : (
                    <div className="space-y-2">
                      <Button
                        className="w-full"
                        onClick={() => handleConnect(channel.id)}
                        disabled={isConnecting}
                      >
                        {isConnecting ? (
                          <>
                            <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                            연동 중...
                          </>
                        ) : (
                          <>
                            <ExternalLink className="w-4 h-4 mr-2" />
                            OAuth로 연동하기
                          </>
                        )}
                      </Button>
                      <Button
                        variant="outline"
                        className="w-full text-xs"
                        onClick={() => handleDevConnect(channel.id)}
                      >
                        개발 모드 (Mock)
                      </Button>
                    </div>
                  )}
                </CardContent>
              </Card>
            );
          })}
        </div>
      </div>

      {/* Dev Mode Modal */}
      {showDevModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 w-full max-w-md mx-4">
            <div className="flex justify-between items-center mb-4">
              <h3 className="text-lg font-semibold">
                {availableChannels.find(c => c.id === showDevModal)?.name} 개발 모드 연동
              </h3>
              <button onClick={() => setShowDevModal(null)} className="text-gray-500 hover:text-gray-700">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="mb-4 p-3 bg-yellow-50 border border-yellow-200 rounded-lg">
              <p className="text-sm text-yellow-700">
                개발 환경에서 테스트용으로 사용합니다. 실제 API와 연동되지 않습니다.
              </p>
            </div>

            <div className="mb-4">
              <Label htmlFor="channel-input">
                {availableChannels.find(c => c.id === showDevModal)?.inputLabel}
              </Label>
              <Input
                id="channel-input"
                placeholder={availableChannels.find(c => c.id === showDevModal)?.inputPlaceholder}
                value={inputValue}
                onChange={(e) => setInputValue(e.target.value)}
                className="mt-1.5"
              />
            </div>

            <div className="flex gap-3">
              <Button
                variant="outline"
                className="flex-1"
                onClick={() => setShowDevModal(null)}
              >
                취소
              </Button>
              <Button
                className="flex-1"
                onClick={handleSubmitDevConnect}
                disabled={!inputValue.trim() || isConnecting}
              >
                {isConnecting ? (
                  <>
                    <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                    연동 중...
                  </>
                ) : (
                  '연동하기'
                )}
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* Connected Channels */}
      {channels.length > 0 && (
        <div className="mb-8">
          <h2 className="text-gray-900 mb-4">연동된 채널</h2>
          <Card>
            <CardContent className="p-6">
              <div className="space-y-4">
                {channels.map((channel) => (
                  <div key={channel.id} className="flex items-start justify-between p-4 rounded-lg border border-gray-200">
                    <div className="flex-1 grid grid-cols-2 md:grid-cols-4 gap-4">
                      <div>
                        <p className="text-xs text-gray-500 mb-1">채널</p>
                        <p className="text-gray-900">{channel.channel_type}</p>
                      </div>
                      <div>
                        <p className="text-xs text-gray-500 mb-1">계정 ID</p>
                        <p className="text-gray-900">{channel.account_id || '-'}</p>
                      </div>
                      <div>
                        <p className="text-xs text-gray-500 mb-1">토큰 만료일</p>
                        <p className="text-gray-900">{channel.token_expires_at ? formatDate(channel.token_expires_at) : '-'}</p>
                      </div>
                      <div>
                        <p className="text-xs text-gray-500 mb-1">상태</p>
                        <Badge className={channel.is_active ? "bg-green-50 text-green-700 border-green-200" : "bg-red-50 text-red-700 border-red-200"}>
                          {channel.is_active ? (
                            <>
                              <CheckCircle className="w-3 h-3 mr-1" />
                              정상
                            </>
                          ) : (
                            <>
                              <AlertCircle className="w-3 h-3 mr-1" />
                              비활성
                            </>
                          )}
                        </Badge>
                      </div>
                    </div>
                    <div className="flex gap-2 ml-4">
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => handleRefreshToken(channel.id)}
                      >
                        <RefreshCw className="w-4 h-4" />
                      </Button>
                      <Button
                        variant="destructive"
                        size="sm"
                        onClick={() => handleDisconnect(channel.id)}
                      >
                        연동 해제
                      </Button>
                    </div>
                  </div>
                ))}
              </div>
            </CardContent>
          </Card>
        </div>
      )}

      {/* Guide Section */}
      <Card>
        <CardHeader>
          <CardTitle>채널 연동 안내</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            <div>
              <h3 className="text-gray-900 mb-2">Instagram 연동 방법</h3>
              <ol className="list-decimal list-inside space-y-1 text-gray-600">
                <li>Instagram 비즈니스 계정이 필요합니다</li>
                <li>Facebook 페이지와 연결되어 있어야 합니다</li>
                <li>연동하기 버튼을 클릭하여 권한을 승인합니다</li>
                <li>웹훅 URL을 Instagram 설정에 추가합니다</li>
              </ol>
            </div>
            <div className="pt-4 border-t border-gray-200">
              <Button variant="link" className="p-0">
                자세한 가이드 보기
                <ExternalLink className="w-4 h-4 ml-1" />
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
