import { useState } from 'react';
import { AuthCard } from '../../components/auth/AuthCard';
import { Button } from '../../components/ui/button';
import { Input } from '../../components/ui/input';

interface ResetErrors {
  password?: string;
  confirmPassword?: string;
}

export function ResetPasswordPage() {
  const [form, setForm] = useState({ password: '', confirmPassword: '' });
  const [errors, setErrors] = useState<ResetErrors>({});
  const [message, setMessage] = useState('');

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

  const handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setMessage('');

    if (validate()) {
      setMessage('Password reset preview accepted. Your new credentials are ready for API integration.');
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

        <Button type="submit" className="w-full">
          Update password
        </Button>
      </form>
    </AuthCard>
  );
}
