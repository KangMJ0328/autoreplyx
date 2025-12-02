import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { ChevronRight, ChevronLeft, Check, Loader2, Instagram, MessageCircle, Phone, Store, Calendar, Shield, Smile, User } from 'lucide-react';
import { Card, CardContent } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import api from '../lib/api';

interface OnboardingData {
  // Step 1: 계정 연결
  instagram_url: string;
  facebook_connected: boolean;
  naver_talktalk_id: string;
  kakao_channel_id: string;
  // Step 2: 상품/서비스
  products_services: string;
  price_menu: string;
  business_hours: string;
  location: string;
  faq_list: string;
  // Step 3: 예약/상담
  reservation_link: string;
  reservation_method: string;
  required_info: string;
  // Step 4: 비즈니스 규칙
  immediate_response_keywords: string;
  banned_topics: string;
  night_auto_response: boolean;
  night_response_message: string;
  // Step 5: 말투/톤
  tone_style: string;
  tone_example: string;
  // Step 6: 관리자 정보
  admin_email: string;
  admin_kakao: string;
  alert_keywords: string;
}

const initialData: OnboardingData = {
  instagram_url: '',
  facebook_connected: false,
  naver_talktalk_id: '',
  kakao_channel_id: '',
  products_services: '',
  price_menu: '',
  business_hours: '',
  location: '',
  faq_list: '',
  reservation_link: '',
  reservation_method: '',
  required_info: '',
  immediate_response_keywords: '',
  banned_topics: '',
  night_auto_response: false,
  night_response_message: '',
  tone_style: 'friendly',
  tone_example: '',
  admin_email: '',
  admin_kakao: '',
  alert_keywords: '',
};

const steps = [
  { id: 1, title: '계정 연결', icon: Instagram, description: 'SNS 계정 연결 정보' },
  { id: 2, title: '상품/서비스', icon: Store, description: '비즈니스 정보 입력' },
  { id: 3, title: '예약/상담', icon: Calendar, description: '예약 방식 설정' },
  { id: 4, title: '비즈니스 규칙', icon: Shield, description: '응답 규칙 설정' },
  { id: 5, title: '말투 설정', icon: Smile, description: '톤 & 매너 설정' },
  { id: 6, title: '관리자 정보', icon: User, description: '알림 설정' },
];

export default function Onboarding() {
  const navigate = useNavigate();
  const [currentStep, setCurrentStep] = useState(1);
  const [data, setData] = useState<OnboardingData>(initialData);
  const [isLoading, setIsLoading] = useState(false);
  const [isSaving, setIsSaving] = useState(false);

  useEffect(() => {
    // 기존 데이터 로드
    const loadProfile = async () => {
      try {
        setIsLoading(true);
        const response = await api.get('/onboarding/profile');
        const profile = response.data;

        if (profile.onboarding_completed) {
          navigate('/');
          return;
        }

        setData({
          ...initialData,
          ...Object.fromEntries(
            Object.entries(profile).filter(([_, v]) => v !== null)
          ),
        });
        setCurrentStep(profile.onboarding_step || 1);
      } catch (error) {
        console.error('Failed to load profile:', error);
      } finally {
        setIsLoading(false);
      }
    };

    loadProfile();
  }, [navigate]);

  const handleChange = (field: keyof OnboardingData, value: string | boolean) => {
    setData((prev) => ({ ...prev, [field]: value }));
  };

  const saveStep = async () => {
    setIsSaving(true);
    try {
      const stepData = getStepData(currentStep);
      await api.post(`/onboarding/step/${currentStep}`, stepData);
      return true;
    } catch (error) {
      console.error('Failed to save step:', error);
      return false;
    } finally {
      setIsSaving(false);
    }
  };

  const getStepData = (step: number) => {
    switch (step) {
      case 1:
        return {
          instagram_url: data.instagram_url,
          facebook_connected: data.facebook_connected,
          naver_talktalk_id: data.naver_talktalk_id,
          kakao_channel_id: data.kakao_channel_id,
        };
      case 2:
        return {
          products_services: data.products_services,
          price_menu: data.price_menu,
          business_hours: data.business_hours,
          location: data.location,
          faq_list: data.faq_list,
        };
      case 3:
        return {
          reservation_link: data.reservation_link,
          reservation_method: data.reservation_method,
          required_info: data.required_info,
        };
      case 4:
        return {
          immediate_response_keywords: data.immediate_response_keywords,
          banned_topics: data.banned_topics,
          night_auto_response: data.night_auto_response,
          night_response_message: data.night_response_message,
        };
      case 5:
        return {
          tone_style: data.tone_style,
          tone_example: data.tone_example,
        };
      case 6:
        return {
          admin_email: data.admin_email,
          admin_kakao: data.admin_kakao,
          alert_keywords: data.alert_keywords,
        };
      default:
        return {};
    }
  };

  const handleNext = async () => {
    const success = await saveStep();
    if (success) {
      if (currentStep < 6) {
        setCurrentStep((prev) => prev + 1);
      } else {
        navigate('/');
      }
    }
  };

  const handlePrev = () => {
    if (currentStep > 1) {
      setCurrentStep((prev) => prev - 1);
    }
  };

  const handleSkip = async () => {
    try {
      await api.post('/onboarding/skip');
      navigate('/');
    } catch (error) {
      console.error('Failed to skip onboarding:', error);
    }
  };

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <Loader2 className="w-8 h-8 text-blue-500 animate-spin" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="max-w-3xl mx-auto px-4">
        {/* Header */}
        <div className="text-center mb-8">
          <h1 className="text-2xl font-bold text-gray-900 mb-2">비즈니스 설정</h1>
          <p className="text-gray-600">자동응답 서비스를 설정하기 위한 정보를 입력해주세요</p>
        </div>

        {/* Progress Steps */}
        <div className="flex items-center justify-between mb-8 px-4">
          {steps.map((step, index) => (
            <div key={step.id} className="flex items-center">
              <div className="flex flex-col items-center">
                <div
                  className={`w-10 h-10 rounded-full flex items-center justify-center ${
                    currentStep > step.id
                      ? 'bg-green-500 text-white'
                      : currentStep === step.id
                      ? 'bg-blue-600 text-white'
                      : 'bg-gray-200 text-gray-500'
                  }`}
                >
                  {currentStep > step.id ? (
                    <Check className="w-5 h-5" />
                  ) : (
                    <step.icon className="w-5 h-5" />
                  )}
                </div>
                <span className={`text-xs mt-1 ${currentStep === step.id ? 'text-blue-600 font-medium' : 'text-gray-500'}`}>
                  {step.title}
                </span>
              </div>
              {index < steps.length - 1 && (
                <div
                  className={`w-8 h-0.5 mx-1 ${
                    currentStep > step.id ? 'bg-green-500' : 'bg-gray-200'
                  }`}
                />
              )}
            </div>
          ))}
        </div>

        {/* Step Content */}
        <Card className="mb-6">
          <CardContent className="p-6">
            <div className="mb-6">
              <h2 className="text-lg font-semibold text-gray-900 mb-1">
                {steps[currentStep - 1].title}
              </h2>
              <p className="text-gray-500 text-sm">{steps[currentStep - 1].description}</p>
            </div>

            {currentStep === 1 && (
              <Step1AccountConnection data={data} onChange={handleChange} />
            )}
            {currentStep === 2 && (
              <Step2ProductService data={data} onChange={handleChange} />
            )}
            {currentStep === 3 && (
              <Step3Reservation data={data} onChange={handleChange} />
            )}
            {currentStep === 4 && (
              <Step4BusinessRules data={data} onChange={handleChange} />
            )}
            {currentStep === 5 && (
              <Step5ToneSetting data={data} onChange={handleChange} />
            )}
            {currentStep === 6 && (
              <Step6AdminInfo data={data} onChange={handleChange} />
            )}
          </CardContent>
        </Card>

        {/* Navigation Buttons */}
        <div className="flex items-center justify-between">
          <Button
            variant="outline"
            onClick={handlePrev}
            disabled={currentStep === 1}
          >
            <ChevronLeft className="w-4 h-4 mr-1" />
            이전
          </Button>

          <Button variant="ghost" onClick={handleSkip}>
            나중에 설정하기
          </Button>

          <Button onClick={handleNext} disabled={isSaving}>
            {isSaving ? (
              <Loader2 className="w-4 h-4 mr-1 animate-spin" />
            ) : currentStep === 6 ? (
              '완료'
            ) : (
              <>
                다음
                <ChevronRight className="w-4 h-4 ml-1" />
              </>
            )}
          </Button>
        </div>
      </div>
    </div>
  );
}

// Step 1: 계정 연결 정보
function Step1AccountConnection({ data, onChange }: { data: OnboardingData; onChange: (field: keyof OnboardingData, value: string | boolean) => void }) {
  return (
    <div className="space-y-4">
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">
          <Instagram className="w-4 h-4 inline mr-1" />
          인스타그램 비즈니스 계정 URL
        </label>
        <Input
          value={data.instagram_url}
          onChange={(e) => onChange('instagram_url', e.target.value)}
          placeholder="https://instagram.com/your_business"
        />
      </div>

      <div>
        <label className="flex items-center gap-2 text-sm font-medium text-gray-700">
          <input
            type="checkbox"
            checked={data.facebook_connected}
            onChange={(e) => onChange('facebook_connected', e.target.checked)}
            className="rounded border-gray-300"
          />
          페이스북 페이지 연결됨
        </label>
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">
          <MessageCircle className="w-4 h-4 inline mr-1" />
          네이버 톡톡 상담센터 ID
        </label>
        <Input
          value={data.naver_talktalk_id}
          onChange={(e) => onChange('naver_talktalk_id', e.target.value)}
          placeholder="네이버 톡톡 ID"
        />
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">
          <Phone className="w-4 h-4 inline mr-1" />
          카카오 채널 ID
        </label>
        <Input
          value={data.kakao_channel_id}
          onChange={(e) => onChange('kakao_channel_id', e.target.value)}
          placeholder="카카오 채널 ID"
        />
      </div>

      <p className="text-xs text-gray-500 mt-4">
        * 연결할 계정이 없는 항목은 비워두셔도 됩니다.
      </p>
    </div>
  );
}

// Step 2: 상품/서비스 정보
function Step2ProductService({ data, onChange }: { data: OnboardingData; onChange: (field: keyof OnboardingData, value: string | boolean) => void }) {
  return (
    <div className="space-y-4">
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">
          제품/서비스 리스트 (간단 설명 포함)
        </label>
        <textarea
          value={data.products_services}
          onChange={(e) => onChange('products_services', e.target.value)}
          placeholder="예: 1. 헤어컷 - 기본 커트 서비스&#10;2. 염색 - 전체/부분 염색&#10;3. 펌 - 볼륨펌, 매직셋팅 등"
          className="w-full min-h-[100px] px-3 py-2 border border-gray-300 rounded-md text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent"
        />
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">
          가격표 / 메뉴판
        </label>
        <textarea
          value={data.price_menu}
          onChange={(e) => onChange('price_menu', e.target.value)}
          placeholder="예: 헤어컷 20,000원 / 염색 50,000원~ / 펌 80,000원~"
          className="w-full min-h-[80px] px-3 py-2 border border-gray-300 rounded-md text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent"
        />
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">
          영업시간
        </label>
        <Input
          value={data.business_hours}
          onChange={(e) => onChange('business_hours', e.target.value)}
          placeholder="예: 평일 10:00-20:00, 토요일 10:00-18:00, 일요일 휴무"
        />
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">
          위치 / 주소
        </label>
        <Input
          value={data.location}
          onChange={(e) => onChange('location', e.target.value)}
          placeholder="예: 서울시 강남구 테헤란로 123 4층"
        />
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">
          자주 묻는 질문 (FAQ)
        </label>
        <textarea
          value={data.faq_list}
          onChange={(e) => onChange('faq_list', e.target.value)}
          placeholder="예:&#10;Q: 주차 가능한가요?&#10;A: 네, 건물 지하 주차장 2시간 무료입니다.&#10;&#10;Q: 예약 없이 방문 가능한가요?&#10;A: 가능하지만, 예약 손님 우선입니다."
          className="w-full min-h-[120px] px-3 py-2 border border-gray-300 rounded-md text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent"
        />
      </div>
    </div>
  );
}

// Step 3: 예약/상담 방식
function Step3Reservation({ data, onChange }: { data: OnboardingData; onChange: (field: keyof OnboardingData, value: string | boolean) => void }) {
  return (
    <div className="space-y-4">
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">
          예약 링크 (있는 경우)
        </label>
        <Input
          value={data.reservation_link}
          onChange={(e) => onChange('reservation_link', e.target.value)}
          placeholder="https://booking.example.com/mybusiness"
        />
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">
          예약 방식 설명
        </label>
        <textarea
          value={data.reservation_method}
          onChange={(e) => onChange('reservation_method', e.target.value)}
          placeholder="예: DM으로 원하시는 날짜/시간을 알려주시면 확인 후 답변드립니다. 또는 네이버 예약으로도 가능합니다."
          className="w-full min-h-[80px] px-3 py-2 border border-gray-300 rounded-md text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent"
        />
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">
          상담 시 필요한 필수 정보
        </label>
        <textarea
          value={data.required_info}
          onChange={(e) => onChange('required_info', e.target.value)}
          placeholder="예:&#10;- 성함&#10;- 연락처&#10;- 희망 날짜/시간&#10;- 원하는 서비스"
          className="w-full min-h-[80px] px-3 py-2 border border-gray-300 rounded-md text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent"
        />
      </div>
    </div>
  );
}

// Step 4: 비즈니스 규칙
function Step4BusinessRules({ data, onChange }: { data: OnboardingData; onChange: (field: keyof OnboardingData, value: string | boolean) => void }) {
  return (
    <div className="space-y-4">
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">
          즉시 응답해야 하는 키워드
        </label>
        <textarea
          value={data.immediate_response_keywords}
          onChange={(e) => onChange('immediate_response_keywords', e.target.value)}
          placeholder="예: 긴급, 급합니다, 오늘, 지금, 바로"
          className="w-full min-h-[60px] px-3 py-2 border border-gray-300 rounded-md text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent"
        />
        <p className="text-xs text-gray-500 mt-1">쉼표로 구분해주세요</p>
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">
          응답하면 안 되는 정보 (금지 항목)
        </label>
        <textarea
          value={data.banned_topics}
          onChange={(e) => onChange('banned_topics', e.target.value)}
          placeholder="예: 개인정보, 계좌번호, 비밀번호, 내부 직원 정보"
          className="w-full min-h-[60px] px-3 py-2 border border-gray-300 rounded-md text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent"
        />
      </div>

      <div className="border-t pt-4">
        <label className="flex items-center gap-2 text-sm font-medium text-gray-700">
          <input
            type="checkbox"
            checked={data.night_auto_response}
            onChange={(e) => onChange('night_auto_response', e.target.checked)}
            className="rounded border-gray-300"
          />
          야간 자동응답 활성화 (영업시간 외)
        </label>
      </div>

      {data.night_auto_response && (
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            야간 자동응답 메시지
          </label>
          <textarea
            value={data.night_response_message}
            onChange={(e) => onChange('night_response_message', e.target.value)}
            placeholder="예: 안녕하세요! 현재 영업시간이 아닙니다. 영업시간 내에 답변 드리겠습니다. 감사합니다!"
            className="w-full min-h-[60px] px-3 py-2 border border-gray-300 rounded-md text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          />
        </div>
      )}
    </div>
  );
}

// Step 5: 말투/톤 설정
function Step5ToneSetting({ data, onChange }: { data: OnboardingData; onChange: (field: keyof OnboardingData, value: string | boolean) => void }) {
  const toneOptions = [
    { value: 'formal', label: '격식체 (존댓말)', example: '안녕하십니까. 문의해 주셔서 감사합니다.' },
    { value: 'friendly', label: '친근한 톤', example: '안녕하세요! 문의 주셔서 감사해요 :)' },
    { value: 'casual', label: '캐주얼 톤', example: '안녕하세요~ 무엇이든 물어보세요!' },
  ];

  return (
    <div className="space-y-4">
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-3">
          응답 말투 선택
        </label>
        <div className="space-y-3">
          {toneOptions.map((option) => (
            <label
              key={option.value}
              className={`block p-4 border rounded-lg cursor-pointer transition ${
                data.tone_style === option.value
                  ? 'border-blue-500 bg-blue-50'
                  : 'border-gray-200 hover:border-gray-300'
              }`}
            >
              <div className="flex items-center gap-2">
                <input
                  type="radio"
                  name="tone_style"
                  value={option.value}
                  checked={data.tone_style === option.value}
                  onChange={(e) => onChange('tone_style', e.target.value)}
                  className="text-blue-600"
                />
                <span className="font-medium">{option.label}</span>
              </div>
              <p className="text-sm text-gray-500 mt-1 ml-6">예시: {option.example}</p>
            </label>
          ))}
        </div>
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">
          원하는 메시지 톤 예시 (선택)
        </label>
        <textarea
          value={data.tone_example}
          onChange={(e) => onChange('tone_example', e.target.value)}
          placeholder="원하시는 응답 스타일이 있다면 예시 메시지를 작성해주세요."
          className="w-full min-h-[80px] px-3 py-2 border border-gray-300 rounded-md text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent"
        />
      </div>
    </div>
  );
}

// Step 6: 관리자 정보
function Step6AdminInfo({ data, onChange }: { data: OnboardingData; onChange: (field: keyof OnboardingData, value: string | boolean) => void }) {
  return (
    <div className="space-y-4">
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">
          담당자 이메일
        </label>
        <Input
          type="email"
          value={data.admin_email}
          onChange={(e) => onChange('admin_email', e.target.value)}
          placeholder="admin@example.com"
        />
        <p className="text-xs text-gray-500 mt-1">중요 알림을 받을 이메일 주소</p>
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">
          담당자 카카오톡 ID
        </label>
        <Input
          value={data.admin_kakao}
          onChange={(e) => onChange('admin_kakao', e.target.value)}
          placeholder="카카오톡 ID"
        />
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">
          중요 메시지 알림 기준
        </label>
        <textarea
          value={data.alert_keywords}
          onChange={(e) => onChange('alert_keywords', e.target.value)}
          placeholder="예: 불만, 환불, 클레임, 긴급, 사장님"
          className="w-full min-h-[60px] px-3 py-2 border border-gray-300 rounded-md text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent"
        />
        <p className="text-xs text-gray-500 mt-1">이 키워드가 포함된 메시지는 즉시 알림을 보내드립니다</p>
      </div>

      <div className="bg-blue-50 p-4 rounded-lg mt-6">
        <h3 className="font-medium text-blue-900 mb-2">설정 완료 안내</h3>
        <p className="text-sm text-blue-700">
          모든 정보는 언제든지 설정 페이지에서 수정할 수 있습니다.
          입력한 정보를 바탕으로 자동응답 규칙이 생성됩니다.
        </p>
      </div>
    </div>
  );
}
