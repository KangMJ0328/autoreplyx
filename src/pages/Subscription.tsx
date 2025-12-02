import { useState, useEffect } from 'react';
import { Check, X, Loader2, Crown, Zap, Building2, AlertCircle, RefreshCw } from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import { subscriptionAPI, Subscription } from '../lib/api';

const PLANS = [
  {
    id: 'free',
    name: '무료',
    price: 0,
    icon: Zap,
    description: '시작하기 좋은 기본 플랜',
    features: [
      { text: '일일 메시지 100건', included: true },
      { text: '자동응답 규칙 5개', included: true },
      { text: '채널 1개 연동', included: true },
      { text: 'AI 자동응답', included: false },
      { text: '우선 지원', included: false },
      { text: '분석 리포트', included: false },
    ],
    buttonText: '현재 플랜',
    buttonVariant: 'outline' as const,
  },
  {
    id: 'pro',
    name: '프로',
    price: 29000,
    icon: Crown,
    description: '성장하는 비즈니스를 위한 플랜',
    features: [
      { text: '일일 메시지 1,000건', included: true },
      { text: '자동응답 규칙 무제한', included: true },
      { text: '채널 3개 연동', included: true },
      { text: 'AI 자동응답', included: true },
      { text: '우선 지원', included: true },
      { text: '분석 리포트', included: false },
    ],
    buttonText: '업그레이드',
    buttonVariant: 'default' as const,
    popular: true,
  },
  {
    id: 'agency',
    name: '에이전시',
    price: 99000,
    icon: Building2,
    description: '대규모 운영을 위한 플랜',
    features: [
      { text: '일일 메시지 무제한', included: true },
      { text: '자동응답 규칙 무제한', included: true },
      { text: '채널 무제한 연동', included: true },
      { text: 'AI 자동응답', included: true },
      { text: '우선 지원', included: true },
      { text: '분석 리포트', included: true },
    ],
    buttonText: '문의하기',
    buttonVariant: 'default' as const,
  },
];

export default function SubscriptionPage() {
  const [subscription, setSubscription] = useState<Subscription | null>(null);
  const [usage, setUsage] = useState<{
    today_messages: number;
    daily_limit: number | null;
    ai_tokens_used: number;
    plan: string;
  } | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isUpgrading, setIsUpgrading] = useState<string | null>(null);
  const [isCancelling, setIsCancelling] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchData = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const [subData, usageData] = await Promise.all([
        subscriptionAPI.get(),
        subscriptionAPI.getUsage(),
      ]);
      setSubscription(subData);
      setUsage(usageData);
    } catch (err) {
      setError('구독 정보를 불러오는데 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleUpgrade = async (plan: 'pro' | 'agency') => {
    setIsUpgrading(plan);
    try {
      const { checkout_url } = await subscriptionAPI.upgrade(plan);
      // 실제로는 Stripe checkout으로 리다이렉트
      window.open(checkout_url, '_blank');
    } catch (err) {
      setError('업그레이드 요청에 실패했습니다.');
    } finally {
      setIsUpgrading(null);
    }
  };

  const handleCancel = async () => {
    if (!confirm('정말 구독을 취소하시겠습니까? 현재 결제 기간이 끝나면 무료 플랜으로 전환됩니다.')) {
      return;
    }

    setIsCancelling(true);
    try {
      await subscriptionAPI.cancel();
      await fetchData();
    } catch (err) {
      setError('구독 취소에 실패했습니다.');
    } finally {
      setIsCancelling(false);
    }
  };

  const handleResume = async () => {
    setIsCancelling(true);
    try {
      await subscriptionAPI.resume();
      await fetchData();
    } catch (err) {
      setError('구독 재개에 실패했습니다.');
    } finally {
      setIsCancelling(false);
    }
  };

  if (isLoading) {
    return (
      <div className="p-8 flex items-center justify-center h-full">
        <div className="text-center">
          <Loader2 className="w-8 h-8 text-blue-500 animate-spin mx-auto mb-4" />
          <p className="text-gray-600">구독 정보를 불러오는 중...</p>
        </div>
      </div>
    );
  }

  const currentPlan = subscription?.plan || 'free';

  return (
    <div className="p-8">
      {/* Header */}
      <div className="mb-8">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-gray-900 mb-2">구독 관리</h1>
            <p className="text-gray-600">요금제를 선택하고 구독을 관리하세요</p>
          </div>
          <Button variant="outline" size="sm" onClick={fetchData}>
            <RefreshCw className="w-4 h-4 mr-2" />
            새로고침
          </Button>
        </div>
      </div>

      {error && (
        <div className="mb-6 p-3 bg-red-50 border border-red-200 rounded-lg text-red-600 text-sm flex items-center gap-2">
          <AlertCircle className="w-4 h-4" />
          {error}
        </div>
      )}

      {/* Current Plan Status */}
      {subscription && (
        <Card className="mb-8">
          <CardHeader>
            <CardTitle>현재 구독</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-4">
                <div className="w-12 h-12 rounded-lg bg-gradient-to-br from-blue-500 to-purple-600 flex items-center justify-center">
                  <Crown className="w-6 h-6 text-white" />
                </div>
                <div>
                  <h3 className="text-lg font-semibold text-gray-900">
                    {PLANS.find(p => p.id === currentPlan)?.name || '무료'} 플랜
                  </h3>
                  <div className="flex items-center gap-2 mt-1">
                    <Badge variant={subscription.status === 'active' ? 'default' : 'secondary'}>
                      {subscription.status === 'active' ? '활성' :
                       subscription.status === 'cancelled' ? '취소됨' :
                       subscription.status === 'trialing' ? '체험중' : subscription.status}
                    </Badge>
                    {subscription.current_period_end && (
                      <span className="text-sm text-gray-500">
                        다음 결제일: {new Date(subscription.current_period_end).toLocaleDateString()}
                      </span>
                    )}
                  </div>
                </div>
              </div>
              <div className="text-right">
                {currentPlan !== 'free' && subscription.status === 'active' && (
                  <Button
                    variant="outline"
                    onClick={handleCancel}
                    disabled={isCancelling}
                  >
                    {isCancelling ? (
                      <Loader2 className="w-4 h-4 animate-spin" />
                    ) : (
                      '구독 취소'
                    )}
                  </Button>
                )}
                {subscription.status === 'cancelled' && (
                  <Button
                    variant="outline"
                    onClick={handleResume}
                    disabled={isCancelling}
                  >
                    {isCancelling ? (
                      <Loader2 className="w-4 h-4 animate-spin" />
                    ) : (
                      '구독 재개'
                    )}
                  </Button>
                )}
              </div>
            </div>

            {/* Usage Stats */}
            {usage && (
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mt-6 pt-6 border-t border-gray-200">
                <div className="text-center p-4 bg-gray-50 rounded-lg">
                  <p className="text-sm text-gray-600 mb-1">오늘 메시지</p>
                  <p className="text-2xl font-bold text-gray-900">
                    {usage.today_messages}
                    <span className="text-sm font-normal text-gray-500">
                      {usage.daily_limit ? ` / ${usage.daily_limit}` : ' / 무제한'}
                    </span>
                  </p>
                  {usage.daily_limit && (
                    <div className="w-full bg-gray-200 rounded-full h-2 mt-2">
                      <div
                        className="bg-blue-500 h-2 rounded-full transition-all"
                        style={{ width: `${Math.min((usage.today_messages / usage.daily_limit) * 100, 100)}%` }}
                      />
                    </div>
                  )}
                </div>
                <div className="text-center p-4 bg-gray-50 rounded-lg">
                  <p className="text-sm text-gray-600 mb-1">이번 달 AI 토큰</p>
                  <p className="text-2xl font-bold text-gray-900">
                    {usage.ai_tokens_used.toLocaleString()}
                  </p>
                </div>
                <div className="text-center p-4 bg-gray-50 rounded-lg">
                  <p className="text-sm text-gray-600 mb-1">현재 플랜</p>
                  <p className="text-2xl font-bold text-gray-900">
                    {PLANS.find(p => p.id === usage.plan)?.name || '무료'}
                  </p>
                </div>
              </div>
            )}
          </CardContent>
        </Card>
      )}

      {/* Pricing Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {PLANS.map((plan) => {
          const Icon = plan.icon;
          const isCurrent = currentPlan === plan.id;

          return (
            <Card
              key={plan.id}
              className={`relative ${plan.popular ? 'border-blue-500 border-2' : ''}`}
            >
              {plan.popular && (
                <div className="absolute -top-3 left-1/2 -translate-x-1/2">
                  <Badge className="bg-blue-500 hover:bg-blue-500">인기</Badge>
                </div>
              )}
              <CardHeader>
                <div className="flex items-center gap-3 mb-2">
                  <div className={`w-10 h-10 rounded-lg flex items-center justify-center ${
                    plan.id === 'free' ? 'bg-gray-100' :
                    plan.id === 'pro' ? 'bg-blue-100' : 'bg-purple-100'
                  }`}>
                    <Icon className={`w-5 h-5 ${
                      plan.id === 'free' ? 'text-gray-600' :
                      plan.id === 'pro' ? 'text-blue-600' : 'text-purple-600'
                    }`} />
                  </div>
                  <div>
                    <CardTitle>{plan.name}</CardTitle>
                    <CardDescription>{plan.description}</CardDescription>
                  </div>
                </div>
                <div className="mt-4">
                  <span className="text-3xl font-bold text-gray-900">
                    {plan.price === 0 ? '무료' : `${plan.price.toLocaleString()}원`}
                  </span>
                  {plan.price > 0 && (
                    <span className="text-gray-500 ml-1">/ 월</span>
                  )}
                </div>
              </CardHeader>
              <CardContent>
                <ul className="space-y-3 mb-6">
                  {plan.features.map((feature, index) => (
                    <li key={index} className="flex items-center gap-2">
                      {feature.included ? (
                        <Check className="w-4 h-4 text-green-500 flex-shrink-0" />
                      ) : (
                        <X className="w-4 h-4 text-gray-300 flex-shrink-0" />
                      )}
                      <span className={feature.included ? 'text-gray-700' : 'text-gray-400'}>
                        {feature.text}
                      </span>
                    </li>
                  ))}
                </ul>
                <Button
                  className={`w-full ${
                    plan.id === 'pro' && !isCurrent ? 'bg-gradient-to-r from-blue-500 to-purple-600 hover:from-blue-600 hover:to-purple-700' : ''
                  }`}
                  variant={isCurrent ? 'outline' : plan.buttonVariant}
                  disabled={isCurrent || isUpgrading !== null}
                  onClick={() => {
                    if (plan.id === 'agency') {
                      window.open('mailto:support@autoreplyx.com?subject=에이전시 플랜 문의', '_blank');
                    } else if (plan.id !== 'free') {
                      handleUpgrade(plan.id as 'pro' | 'agency');
                    }
                  }}
                >
                  {isUpgrading === plan.id ? (
                    <Loader2 className="w-4 h-4 animate-spin" />
                  ) : isCurrent ? (
                    '현재 플랜'
                  ) : (
                    plan.buttonText
                  )}
                </Button>
              </CardContent>
            </Card>
          );
        })}
      </div>

      {/* FAQ */}
      <Card className="mt-8">
        <CardHeader>
          <CardTitle>자주 묻는 질문</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div>
            <h4 className="font-medium text-gray-900 mb-1">플랜을 언제든 변경할 수 있나요?</h4>
            <p className="text-sm text-gray-600">
              네, 언제든지 플랜을 업그레이드하거나 다운그레이드할 수 있습니다. 업그레이드 시 즉시 적용되며, 다운그레이드는 현재 결제 기간이 끝난 후 적용됩니다.
            </p>
          </div>
          <div>
            <h4 className="font-medium text-gray-900 mb-1">환불 정책은 어떻게 되나요?</h4>
            <p className="text-sm text-gray-600">
              결제 후 7일 이내에 요청 시 전액 환불이 가능합니다. 7일 이후에는 남은 기간에 대한 일할 계산 환불이 적용됩니다.
            </p>
          </div>
          <div>
            <h4 className="font-medium text-gray-900 mb-1">결제 방법은 무엇인가요?</h4>
            <p className="text-sm text-gray-600">
              신용카드, 체크카드로 결제할 수 있으며, 카카오페이, 네이버페이도 지원합니다.
            </p>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
