import { useState } from 'react';
import { Send, MessageSquare, Bot, ListChecks, Loader2, AlertCircle, CheckCircle, Clock, Sparkles } from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
import { Textarea } from '../components/ui/textarea';
import { Badge } from '../components/ui/badge';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../components/ui/select';
import { webhookAPI, aiAPI } from '../lib/api';

interface TestResult {
  success: boolean;
  response_type?: 'rule' | 'ai' | 'none';
  response_text?: string;
  matched_rule?: {
    id: number;
    name: string;
    match_type: string;
    keyword: string;
  } | null;
  ai_tokens_used?: number;
  cached?: boolean;
  error?: string;
  timestamp: Date;
}

const SAMPLE_MESSAGES = [
  '영업시간이 어떻게 되나요?',
  '주차 가능한가요?',
  '예약 가능한가요?',
  '가격이 어떻게 되나요?',
  '위치가 어디인가요?',
  '배달 되나요?',
  '반려동물 출입 가능한가요?',
  '단체 예약 가능한가요?',
];

export default function WebhookTest() {
  const [message, setMessage] = useState('');
  const [senderId, setSenderId] = useState('test_user_123');
  const [channel, setChannel] = useState('instagram');
  const [isLoading, setIsLoading] = useState(false);
  const [testHistory, setTestHistory] = useState<TestResult[]>([]);
  const [error, setError] = useState<string | null>(null);

  const handleTest = async () => {
    if (!message.trim()) return;

    setIsLoading(true);
    setError(null);

    try {
      // AI 테스트 API 호출
      const result = await aiAPI.testMessage(message, channel);

      const testResult: TestResult = {
        success: true,
        response_type: result.response_type,
        response_text: result.response_text,
        matched_rule: result.matched_rule,
        ai_tokens_used: result.ai_tokens_used,
        cached: result.cached,
        timestamp: new Date(),
      };

      setTestHistory((prev) => [testResult, ...prev]);
    } catch (err: any) {
      const testResult: TestResult = {
        success: false,
        error: err.response?.data?.error || err.message || '테스트 실패',
        timestamp: new Date(),
      };
      setTestHistory((prev) => [testResult, ...prev]);
      setError(testResult.error || '테스트 실패');
    } finally {
      setIsLoading(false);
    }
  };

  const handleSampleMessage = (msg: string) => {
    setMessage(msg);
  };

  const clearHistory = () => {
    setTestHistory([]);
  };

  return (
    <div className="p-8">
      {/* Header */}
      <div className="mb-8">
        <h1 className="text-gray-900 mb-2">메시지 테스트</h1>
        <p className="text-gray-600">메시지를 시뮬레이션하여 자동응답을 테스트하세요</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Test Form */}
        <div className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle>테스트 메시지 입력</CardTitle>
              <CardDescription>고객이 보낼 메시지를 입력하고 응답을 미리 확인하세요</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <Label htmlFor="channel">채널</Label>
                  <Select value={channel} onValueChange={setChannel}>
                    <SelectTrigger id="channel" className="mt-1.5">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="instagram">Instagram</SelectItem>
                      <SelectItem value="kakao">카카오톡</SelectItem>
                      <SelectItem value="naver">네이버 톡톡</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
                <div>
                  <Label htmlFor="senderId">발신자 ID</Label>
                  <Input
                    id="senderId"
                    value={senderId}
                    onChange={(e) => setSenderId(e.target.value)}
                    className="mt-1.5"
                    placeholder="test_user"
                  />
                </div>
              </div>

              <div>
                <Label htmlFor="message">메시지</Label>
                <Textarea
                  id="message"
                  value={message}
                  onChange={(e) => setMessage(e.target.value)}
                  placeholder="테스트할 메시지를 입력하세요"
                  rows={4}
                  className="mt-1.5"
                />
              </div>

              <Button
                onClick={handleTest}
                disabled={isLoading || !message.trim()}
                className="w-full bg-gradient-to-r from-blue-500 to-purple-600 hover:from-blue-600 hover:to-purple-700"
              >
                {isLoading ? (
                  <>
                    <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                    테스트 중...
                  </>
                ) : (
                  <>
                    <Send className="w-4 h-4 mr-2" />
                    테스트 실행
                  </>
                )}
              </Button>

              {error && (
                <div className="p-3 bg-red-50 border border-red-200 rounded-lg text-red-600 text-sm flex items-center gap-2">
                  <AlertCircle className="w-4 h-4" />
                  {error}
                </div>
              )}
            </CardContent>
          </Card>

          {/* Sample Messages */}
          <Card>
            <CardHeader>
              <CardTitle>샘플 메시지</CardTitle>
              <CardDescription>자주 사용되는 질문을 빠르게 테스트하세요</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="flex flex-wrap gap-2">
                {SAMPLE_MESSAGES.map((msg, idx) => (
                  <Button
                    key={idx}
                    variant="outline"
                    size="sm"
                    onClick={() => handleSampleMessage(msg)}
                    className="text-sm"
                  >
                    {msg}
                  </Button>
                ))}
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Test Results */}
        <Card>
          <CardHeader>
            <div className="flex items-center justify-between">
              <div>
                <CardTitle>테스트 결과</CardTitle>
                <CardDescription>최근 테스트 결과를 확인하세요</CardDescription>
              </div>
              {testHistory.length > 0 && (
                <Button variant="ghost" size="sm" onClick={clearHistory}>
                  기록 삭제
                </Button>
              )}
            </div>
          </CardHeader>
          <CardContent>
            {testHistory.length === 0 ? (
              <div className="text-center py-12 text-gray-500">
                <MessageSquare className="w-12 h-12 mx-auto mb-4 opacity-50" />
                <p>테스트 결과가 없습니다</p>
                <p className="text-sm mt-1">메시지를 입력하고 테스트를 실행하세요</p>
              </div>
            ) : (
              <div className="space-y-4 max-h-[600px] overflow-y-auto">
                {testHistory.map((result, idx) => (
                  <div
                    key={idx}
                    className={`p-4 rounded-lg border ${
                      result.success ? 'border-gray-200 bg-gray-50' : 'border-red-200 bg-red-50'
                    }`}
                  >
                    {result.success ? (
                      <>
                        <div className="flex items-center gap-2 mb-3">
                          {result.response_type === 'rule' ? (
                            <ListChecks className="w-4 h-4 text-green-500" />
                          ) : result.response_type === 'ai' ? (
                            <Bot className="w-4 h-4 text-purple-500" />
                          ) : (
                            <AlertCircle className="w-4 h-4 text-gray-400" />
                          )}
                          <span className="font-medium text-gray-900">
                            {result.response_type === 'rule' ? '규칙 응답' :
                             result.response_type === 'ai' ? 'AI 응답' : '응답 없음'}
                          </span>
                          {result.response_type === 'rule' && result.matched_rule && (
                            <Badge variant="secondary" className="text-xs">
                              규칙: {result.matched_rule.name}
                            </Badge>
                          )}
                          {result.cached && (
                            <Badge variant="outline" className="text-xs">
                              캐시됨
                            </Badge>
                          )}
                        </div>

                        {result.response_text && (
                          <div className="p-3 bg-white rounded border border-gray-200 mb-3">
                            <p className="text-gray-700 text-sm">{result.response_text}</p>
                          </div>
                        )}

                        <div className="flex items-center gap-4 text-xs text-gray-500">
                          <span className="flex items-center gap-1">
                            <Clock className="w-3 h-3" />
                            {result.timestamp.toLocaleTimeString()}
                          </span>
                          {result.ai_tokens_used !== undefined && result.ai_tokens_used > 0 && (
                            <span className="flex items-center gap-1">
                              <Sparkles className="w-3 h-3" />
                              토큰: {result.ai_tokens_used}
                            </span>
                          )}
                          {result.matched_rule && (
                            <span>
                              매칭: {result.matched_rule.match_type} "{result.matched_rule.keyword}"
                            </span>
                          )}
                        </div>
                      </>
                    ) : (
                      <div className="flex items-center gap-2 text-red-600">
                        <AlertCircle className="w-4 h-4" />
                        <span>{result.error}</span>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Info Card */}
      <Card className="mt-6">
        <CardHeader>
          <CardTitle>테스트 안내</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3 text-sm text-gray-600">
          <div className="flex items-start gap-3">
            <CheckCircle className="w-5 h-5 text-green-500 flex-shrink-0 mt-0.5" />
            <div>
              <p className="font-medium text-gray-900">규칙 우선 매칭</p>
              <p>입력된 메시지는 먼저 자동응답 규칙과 매칭됩니다. 키워드가 일치하면 규칙에 정의된 응답이 반환됩니다.</p>
            </div>
          </div>
          <div className="flex items-start gap-3">
            <Bot className="w-5 h-5 text-purple-500 flex-shrink-0 mt-0.5" />
            <div>
              <p className="font-medium text-gray-900">AI 폴백</p>
              <p>규칙에 매칭되지 않으면 AI가 비즈니스 정보를 바탕으로 자동 응답을 생성합니다.</p>
            </div>
          </div>
          <div className="flex items-start gap-3">
            <Sparkles className="w-5 h-5 text-blue-500 flex-shrink-0 mt-0.5" />
            <div>
              <p className="font-medium text-gray-900">캐시 기능</p>
              <p>동일한 질문에 대한 AI 응답은 24시간 동안 캐시되어 빠른 응답과 토큰 절약이 가능합니다.</p>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
