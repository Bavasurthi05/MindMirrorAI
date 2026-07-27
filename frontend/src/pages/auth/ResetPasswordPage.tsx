import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { AuthCard } from '../../components/auth/AuthCard';
import { Button } from '../../components/ui/button';
import { Input } from '../../components/ui/input';
import { resetPassword } from '../../lib/auth-service';
import { ApiError } from '../../lib/api';

interface ResetErrors {
  password?: string;
  confirmPassword?: string;
}

export function ResetPasswordPage() {
  const [form, setForm] = useState({ password: '', confirmPassword: '' });
  const [errors, setErrors] = useState<ResetErrors>({});
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const token = searchParams.get('token') ?? '';

  const validate = () => {
    const nextErrors: ResetErrors = {};

    if (!form.password) {
      nextErrors.password = 'Password is required.';
    } else if (form.password.length < 8) {
      nextErrors.password = 'Password must be at least 8 characters.';
    }

    if (!form.confirmPassword) {
      nextErrors.confirmPassword = 'Please confirm your password.';
    } else if (form.confirmPassword !== form.password) {
      nextErrors.confirmPassword = 'Passwords do not match.';
    }

    setErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  };

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setMessage('');
    setError('');

    if (!token) {
      setError('This reset link is missing its token. Please request a new one.');
      return;
    }

    if (!validate()) return;

    setSubmitting(true);
    try {
      await resetPassword(token, form.password);
      setMessage('Your password has been updated. Redirecting to sign in…');
      setTimeout(() => navigate('/login', { replace: true }), 1500);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Unable to reset password. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <AuthCard
      title="Reset password"
      description="Choose a fresh password for your account and continue securely."
      footerText="Need a new code?"
      footerLinkText="Try again"
      footerHref="/forgot-password"
    >
      <form className="space-y-4" onSubmit={handleSubmit}>
        <Input
          label="New password"
          type="password"
          placeholder="Enter a new password"
          value={form.password}
          error={errors.password}
          onChange={(event) => {
            setForm({ ...form, password: event.target.value });
            if (errors.password) setErrors({ ...errors, password: undefined });
          }}
        />

        <Input
          label="Confirm password"
          type="password"
          placeholder="Re-enter the password"
          value={form.confirmPassword}
          error={errors.confirmPassword}
          onChange={(event) => {
            setForm({ ...form, confirmPassword: event.target.value });
            if (errors.confirmPassword) setErrors({ ...errors, confirmPassword: undefined });
          }}
        />

        {message ? <p className="rounded-xl bg-emerald-50 px-3 py-2 text-sm text-emerald-700">{message}</p> : null}
        {error ? <p className="rounded-xl bg-rose-50 px-3 py-2 text-sm text-rose-700">{error}</p> : null}

        <Button type="submit" className="w-full" disabled={submitting}>
          {submitting ? 'Updating…' : 'Update password'}
        </Button>
      </form>
    </AuthCard>
  );
}
