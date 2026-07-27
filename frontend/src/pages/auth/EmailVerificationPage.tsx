import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { AuthCard } from '../../components/auth/AuthCard';
import { Button } from '../../components/ui/button';
import { verifyEmail } from '../../lib/auth-service';
import { ApiError } from '../../lib/api';

type Status = 'idle' | 'verifying' | 'success' | 'error';

export function EmailVerificationPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const [status, setStatus] = useState<Status>(token ? 'verifying' : 'idle');
  const [error, setError] = useState('');

  useEffect(() => {
    if (!token) return;
    let cancelled = false;
    setStatus('verifying');
    verifyEmail(token)
      .then(() => {
        if (!cancelled) setStatus('success');
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        setError(err instanceof ApiError ? err.message : 'Verification failed. The link may be expired.');
        setStatus('error');
      });
    return () => {
      cancelled = true;
    };
  }, [token]);

  const content = {
    idle: {
      emoji: '✉️',
      title: 'Check your inbox',
      text: 'We sent you a verification link. Open it to confirm your email address.',
    },
    verifying: {
      emoji: '⏳',
      title: 'Verifying your email…',
      text: 'Hang tight while we confirm your verification link.',
    },
    success: {
      emoji: '✅',
      title: 'Email verified',
      text: 'Your email has been confirmed. You can now sign in and continue.',
    },
    error: {
      emoji: '⚠️',
      title: 'Verification failed',
      text: error || 'This verification link is invalid or has expired.',
    },
  }[status];

  return (
    <AuthCard
      title="Verify your email"
      description="Confirm your email address to secure your account."
      footerText="Already verified?"
      footerLinkText="Go to sign in"
      footerHref="/login"
    >
      <div className="space-y-4 text-center">
        <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-emerald-100 text-3xl">
          {content.emoji}
        </div>
        <div className="space-y-2">
          <p className="text-lg font-semibold text-slate-900">{content.title}</p>
          <p className="text-sm leading-6 text-slate-600">{content.text}</p>
        </div>
        <div className="flex flex-wrap justify-center gap-3">
          <Link to="/login">
            <Button variant="secondary">Back to login</Button>
          </Link>
        </div>
      </div>
    </AuthCard>
  );
}
