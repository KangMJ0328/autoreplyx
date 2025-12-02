import { useState, useEffect } from 'react';
import { Building2, Mail, Phone, MapPin, Clock, FileText, Bot, AlertCircle, Loader2, Send, MessageSquare, Sparkles, Key, Lock } from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
import { Textarea } from '../components/ui/textarea';
import { Switch } from '../components/ui/switch';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../components/ui/select';
import { Badge } from '../components/ui/badge';
import { profileAPI, aiAPI } from '../lib/api';
import { useAuth } from '../contexts/AuthContext';

export default function Profile() {
  const { user, refreshUser } = useAuth();
  const [isLoading, setIsLoading] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const [brandName, setBrandName] = useState('');
  const [industry, setIndustry] = useState('');
  const [businessHours, setBusinessHours] = useState('');
  const [address, setAddress] = useState('');
  const [description, setDescription] = useState('');
  const [reservationSlug, setReservationSlug] = useState('');
  const [aiEnabled, setAiEnabled] = useState(true);
  const [aiTone, setAiTone] = useState('friendly');
  const [bannedWords, setBannedWords] = useState('');

  // AI Test State
  const [testMessage, setTestMessage] = useState('');
  const [testResponse, setTestResponse] = useState<{
    response_type: 'rule' | 'ai';
    response_text: string;
    ai_tokens_used: number;
    cached: boolean;
  } | null>(null);
  const [isTesting, setIsTesting] = useState(false);

  // Password Change State
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [passwordError, setPasswordError] = useState<string | null>(null);
  const [passwordSuccess, setPasswordSuccess] = useState<string | null>(null);
  const [isChangingPassword, setIsChangingPassword] = useState(false);

  useEffect(() => {
    if (user) {
      setBrandName(user.brand_name || '');
      setIndustry(user.industry || '');
      setBusinessHours(user.business_hours || '');
      setAddress(user.address || '');
      setDescription(user.description || '');
      setReservationSlug(user.reservation_slug || '');
      setAiEnabled(user.ai_enabled ?? true);
      setAiTone(user.ai_tone || 'friendly');
      setBannedWords(Array.isArray(user.banned_words) ? user.banned_words.join(', ') : '');
    }
  }, [user]);

  const handleSave = async () => {
    setIsSaving(true);
    setError(null);
    setSuccess(null);

    try {
      await profileAPI.update({
        brand_name: brandName,
        industry,
        business_hours: businessHours,
        address,
        description,
        reservation_slug: reservationSlug,
        ai_enabled: aiEnabled,
        ai_tone: aiTone as any,
        banned_words: bannedWords.split(',').map(w => w.trim()).filter(w => w),
      });

      if (refreshUser) {
        await refreshUser();
      }

      setSuccess('프로필이 저장되었습니다.');
      setTimeout(() => setSuccess(null), 3000);
    } catch (err: any) {
      setError(err.response?.data?.message || '프로필 저장에 실패했습니다.');
    } finally {
      setIsSaving(false);
    }
  };

  const handleTestAI = async () => {
    if (!testMessage.trim()) return;

    setIsTesting(true);
    setTestResponse(null);

    try {
      const result = await aiAPI.testMessage(testMessage);
      setTestResponse(result);
    } catch (err: any) {
      setError(err.response?.data?.message || 'AI 테스트에 실패했습니다.');
    } finally {
      setIsTesting(false);
    }
  };

  const handleChangePassword = async () => {
    setPasswordError(null);
    setPasswordSuccess(null);

    if (newPassword !== confirmPassword) {
      setPasswordError('새 비밀번호가 일치하지 않습니다.');
      return;
    }

    if (newPassword.length < 8) {
      setPasswordError('비밀번호는 8자 이상이어야 합니다.');
      return;
    }

    setIsChangingPassword(true);

    try {
      await profileAPI.updatePassword({
        current_password: currentPassword,
        password: newPassword,
        password_confirmation: confirmPassword,
      });

      setPasswordSuccess('비밀번호가 변경되었습니다.');
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
      setTimeout(() => setPasswordSuccess(null), 3000);
    } catch (err: any) {
      setPasswordError(err.response?.data?.message || '비밀번호 변경에 실패했습니다.');
    } finally {
      setIsChangingPassword(false);
    }
  };

  if (isLoading) {
    return (
      <div className="p-8 flex items-center justify-center h-full">
        <div className="text-center">
          <Loader2 className="w-8 h-8 text-blue-500 animate-spin mx-auto mb-4" />
          <p className="text-gray-600">프로필 정보를 불러오는 중...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="p-8">
      {/* Header */}
      <div className="mb-8">
        <h1 className="text-gray-900 mb-2">프로필 설정</h1>
        <p className="text-gray-600">비즈니스 정보와 AI 설정을 관리하세요</p>
      </div>

      {/* Messages */}
      {error && (
        <div className="mb-6 p-3 bg-red-50 border border-red-200 rounded-lg text-red-600 text-sm">
          {error}
        </div>
      )}
      {success && (
        <div className="mb-6 p-3 bg-green-50 border border-green-200 rounded-lg text-green-600 text-sm">
          {success}
        </div>
      )}

      <div className="space-y-6">
        {/* Basic Information */}
        <Card>
          <CardHeader>
            <CardTitle>기본 정보</CardTitle>
            <CardDescription>비즈니스의 기본 정보를 입력하세요</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <Label htmlFor="email">이메일 (읽기 전용)</Label>
                <div className="relative mt-1.5">
                  <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                  <Input id="email" value={user?.email || ''} readOnly className="pl-10 bg-gray-50" />
                </div>
              </div>
              <div>
                <Label htmlFor="brandName">브랜드명</Label>
                <div className="relative mt-1.5">
                  <Building2 className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                  <Input
                    id="brandName"
                    value={brandName}
                    onChange={(e) => setBrandName(e.target.value)}
                    className="pl-10"
                  />
                </div>
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <Label htmlFor="industry">업종</Label>
                <Select value={industry} onValueChange={setIndustry}>
                  <SelectTrigger id="industry" className="mt-1.5">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="cafe">카페</SelectItem>
                    <SelectItem value="shopping">쇼핑몰</SelectItem>
                    <SelectItem value="beauty">미용실</SelectItem>
                    <SelectItem value="freelance">프리랜서</SelectItem>
                    <SelectItem value="restaurant">음식점</SelectItem>
                    <SelectItem value="other">기타</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div>
                <Label htmlFor="businessHours">영업시간</Label>
                <div className="relative mt-1.5">
                  <Clock className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                  <Input
                    id="businessHours"
                    value={businessHours}
                    onChange={(e) => setBusinessHours(e.target.value)}
                    placeholder="예: 평일 10:00-22:00"
                    className="pl-10"
                  />
                </div>
              </div>
            </div>

            <div>
              <Label htmlFor="address">주소</Label>
              <div className="relative mt-1.5">
                <MapPin className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                <Input
                  id="address"
                  value={address}
                  onChange={(e) => setAddress(e.target.value)}
                  placeholder="주소를 입력하세요"
                  className="pl-10"
                />
              </div>
            </div>

            <div>
              <Label htmlFor="description">비즈니스 소개</Label>
              <div className="relative mt-1.5">
                <FileText className="absolute left-3 top-3 w-5 h-5 text-gray-400" />
                <Textarea
                  id="description"
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder="비즈니스에 대해 간단히 소개해주세요"
                  rows={4}
                  className="pl-10"
                />
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Reservation Page Settings */}
        <Card>
          <CardHeader>
            <CardTitle>예약 페이지 설정</CardTitle>
            <CardDescription>고객이 사용할 예약 페이지 URL을 설정하세요</CardDescription>
          </CardHeader>
          <CardContent>
            <div>
              <Label htmlFor="reservationSlug">예약 페이지 URL</Label>
              <div className="flex items-center gap-2 mt-1.5">
                <span className="text-gray-500">autoreplyx.com/r/</span>
                <Input
                  id="reservationSlug"
                  value={reservationSlug}
                  onChange={(e) => setReservationSlug(e.target.value.toLowerCase().replace(/[^a-z0-9-]/g, ''))}
                  className="flex-1"
                  placeholder="my-brand"
                />
              </div>
              {reservationSlug && (
                <p className="text-xs text-gray-500 mt-1.5">
                  예약 URL: autoreplyx.com/r/{reservationSlug}
                </p>
              )}
            </div>
          </CardContent>
        </Card>

        {/* AI Settings */}
        <Card>
          <CardHeader>
            <CardTitle>AI 설정</CardTitle>
            <CardDescription>AI 자동응답의 동작을 설정하세요</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex items-center justify-between p-4 rounded-lg border border-gray-200">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-lg bg-purple-50 flex items-center justify-center">
                  <Bot className="w-5 h-5 text-purple-600" />
                </div>
                <div>
                  <p className="text-gray-900">AI 자동응답 활성화</p>
                  <p className="text-xs text-gray-500">규칙에 없는 질문을 AI가 자동으로 응답합니다</p>
                </div>
              </div>
              <Switch checked={aiEnabled} onCheckedChange={setAiEnabled} />
            </div>

            {aiEnabled && (
              <>
                <div>
                  <Label htmlFor="aiTone">AI 응답 톤</Label>
                  <Select value={aiTone} onValueChange={setAiTone}>
                    <SelectTrigger id="aiTone" className="mt-1.5">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="professional">전문적인</SelectItem>
                      <SelectItem value="friendly">친근한</SelectItem>
                      <SelectItem value="formal">공손한</SelectItem>
                      <SelectItem value="casual">캐주얼한</SelectItem>
                    </SelectContent>
                  </Select>
                </div>

                <div>
                  <Label htmlFor="bannedWords">금지어</Label>
                  <div className="relative mt-1.5">
                    <AlertCircle className="absolute left-3 top-3 w-5 h-5 text-gray-400" />
                    <Textarea
                      id="bannedWords"
                      value={bannedWords}
                      onChange={(e) => setBannedWords(e.target.value)}
                      placeholder="쉼표로 구분하여 입력하세요 (예: 단어1, 단어2, 단어3)"
                      rows={3}
                      className="pl-10"
                    />
                  </div>
                  <p className="text-xs text-gray-500 mt-1.5">AI가 사용하지 않을 단어를 설정하세요</p>
                </div>

                {/* AI Test Section */}
                <div className="border-t border-gray-200 pt-4 mt-4">
                  <Label htmlFor="testMessage">AI 응답 테스트</Label>
                  <p className="text-xs text-gray-500 mb-2">메시지를 입력해 AI 응답을 미리 확인하세요</p>
                  <div className="flex gap-2">
                    <div className="relative flex-1">
                      <MessageSquare className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                      <Input
                        id="testMessage"
                        value={testMessage}
                        onChange={(e) => setTestMessage(e.target.value)}
                        placeholder="테스트 메시지를 입력하세요"
                        className="pl-10"
                        onKeyPress={(e) => e.key === 'Enter' && handleTestAI()}
                      />
                    </div>
                    <Button
                      onClick={handleTestAI}
                      disabled={isTesting || !testMessage.trim()}
                      className="bg-purple-500 hover:bg-purple-600"
                    >
                      {isTesting ? (
                        <Loader2 className="w-4 h-4 animate-spin" />
                      ) : (
                        <Send className="w-4 h-4" />
                      )}
                    </Button>
                  </div>

                  {testResponse && (
                    <div className="mt-4 p-4 bg-gray-50 rounded-lg">
                      <div className="flex items-center gap-2 mb-2">
                        <Sparkles className="w-4 h-4 text-purple-500" />
                        <span className="font-medium text-gray-900">AI 응답</span>
                        <Badge variant={testResponse.response_type === 'ai' ? 'default' : 'secondary'}>
                          {testResponse.response_type === 'ai' ? 'AI 생성' : '규칙 응답'}
                        </Badge>
                        {testResponse.cached && (
                          <Badge variant="outline">캐시됨</Badge>
                        )}
                      </div>
                      <p className="text-gray-700 text-sm">{testResponse.response_text}</p>
                      {testResponse.ai_tokens_used > 0 && (
                        <p className="text-xs text-gray-500 mt-2">
                          토큰 사용량: {testResponse.ai_tokens_used}
                        </p>
                      )}
                    </div>
                  )}
                </div>
              </>
            )}
          </CardContent>
        </Card>

        {/* Password Change */}
        <Card>
          <CardHeader>
            <CardTitle>비밀번호 변경</CardTitle>
            <CardDescription>계정 보안을 위해 비밀번호를 주기적으로 변경하세요</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            {passwordError && (
              <div className="p-3 bg-red-50 border border-red-200 rounded-lg text-red-600 text-sm">
                {passwordError}
              </div>
            )}
            {passwordSuccess && (
              <div className="p-3 bg-green-50 border border-green-200 rounded-lg text-green-600 text-sm">
                {passwordSuccess}
              </div>
            )}

            <div>
              <Label htmlFor="currentPassword">현재 비밀번호</Label>
              <div className="relative mt-1.5">
                <Key className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                <Input
                  id="currentPassword"
                  type="password"
                  value={currentPassword}
                  onChange={(e) => setCurrentPassword(e.target.value)}
                  placeholder="현재 비밀번호"
                  className="pl-10"
                />
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <Label htmlFor="newPassword">새 비밀번호</Label>
                <div className="relative mt-1.5">
                  <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                  <Input
                    id="newPassword"
                    type="password"
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    placeholder="새 비밀번호 (8자 이상)"
                    className="pl-10"
                  />
                </div>
              </div>
              <div>
                <Label htmlFor="confirmPassword">비밀번호 확인</Label>
                <div className="relative mt-1.5">
                  <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                  <Input
                    id="confirmPassword"
                    type="password"
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    placeholder="새 비밀번호 확인"
                    className="pl-10"
                  />
                </div>
              </div>
            </div>

            <Button
              onClick={handleChangePassword}
              disabled={isChangingPassword || !currentPassword || !newPassword || !confirmPassword}
              variant="outline"
            >
              {isChangingPassword ? (
                <>
                  <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                  변경 중...
                </>
              ) : (
                '비밀번호 변경'
              )}
            </Button>
          </CardContent>
        </Card>

        {/* Actions */}
        <div className="flex justify-end gap-3">
          <Button variant="outline" onClick={() => window.history.back()}>취소</Button>
          <Button
            onClick={handleSave}
            disabled={isSaving}
            className="bg-gradient-to-r from-blue-500 to-purple-600 hover:from-blue-600 hover:to-purple-700"
          >
            {isSaving ? (
              <>
                <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                저장 중...
              </>
            ) : (
              '저장하기'
            )}
          </Button>
        </div>
      </div>
    </div>
  );
}
