import { useState } from 'react';
import { AuthCard } from '../../components/auth/AuthCard';
import { Button } from '../../components/ui/button';
import { Input } from '../../components/ui/input';
import { forgotPassword } from '../../lib/auth-service';
import { ApiError } from '../../lib/api';

export function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError('');
    setMessage('');

    if (!email.trim()) {
      setError('Email is required.');
      return;
    }

    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      setError('Please enter a valid email address.');
      return;
    }

    setSubmitting(true);
    try {
      await forgotPassword(email.trim());
      setMessage('If an account exists for that email, a reset link is on its way.');
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Something went wrong. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <AuthCard
      title="Forgot password"
      description="Enter your email and we will guide you through the recovery flow."
      footerText="Remembered it?"
      footerLinkText="Return to sign in"
      footerHref="/login"
    >
      <form className="space-y-4" onSubmit={handleSubmit}>
        <Input
          label="Email"
          type="email"
          placeholder="you@example.com"
          value={email}
          error={error}
          onChange={(event) => {
            setEmail(event.target.value);
            if (error) setError('');
          }}
        />

        {message ? <p className="rounded-xl bg-emerald-50 px-3 py-2 text-sm text-emerald-700">{message}</p> : null}

        <Button type="submit" className="w-full" disabled={submitting}>
          {submitting ? 'Sending…' : 'Send recovery link'}
        </Button>
      </form>
    </AuthCard>
  );
}
