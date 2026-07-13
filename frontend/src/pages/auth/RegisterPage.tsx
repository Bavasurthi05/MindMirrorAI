import { useState } from 'react';
import { AuthCard } from '../../components/auth/AuthCard';
import { Button } from '../../components/ui/button';
import { Input } from '../../components/ui/input';

interface RegisterErrors {
  name?: string;
  email?: string;
  password?: string;
  confirmPassword?: string;
  terms?: string;
}

export function RegisterPage() {
  const [form, setForm] = useState({
    name: '',
    email: '',
    password: '',
    confirmPassword: '',
    terms: false,
  });
  const [errors, setErrors] = useState<RegisterErrors>({});
  const [message, setMessage] = useState('');

  const validate = () => {
    const nextErrors: RegisterErrors = {};

    if (!form.name.trim()) {
      nextErrors.name = 'Name is required.';
    }

    if (!form.email.trim()) {
      nextErrors.email = 'Email is required.';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
      nextErrors.email = 'Please enter a valid email address.';
    }

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

    if (!form.terms) {
      nextErrors.terms = 'You must accept the terms to continue.';
    }

    setErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  };

  const handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setMessage('');

    if (validate()) {
      setMessage('Front-end validation passed. Account creation flow can be wired to an API later.');
    }
  };

  return (
    <AuthCard
      title="Create account"
      description="Start your private wellness journey with an inviting and simple setup flow."
      footerText="Already have an account?"
      footerLinkText="Sign in"
      footerHref="/login"
    >
      <form className="space-y-4" onSubmit={handleSubmit}>
        <Input
          label="Full name"
          placeholder="Alicia Morgan"
          value={form.name}
          error={errors.name}
          onChange={(event) => {
            setForm({ ...form, name: event.target.value });
            if (errors.name) setErrors({ ...errors, name: undefined });
          }}
        />

        <Input
          label="Email"
          type="email"
          placeholder="you@example.com"
          value={form.email}
          error={errors.email}
          onChange={(event) => {
            setForm({ ...form, email: event.target.value });
            if (errors.email) setErrors({ ...errors, email: undefined });
          }}
        />

        <Input
          label="Password"
          type="password"
          placeholder="Create a secure password"
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
          placeholder="Re-enter your password"
          value={form.confirmPassword}
          error={errors.confirmPassword}
          onChange={(event) => {
            setForm({ ...form, confirmPassword: event.target.value });
            if (errors.confirmPassword) setErrors({ ...errors, confirmPassword: undefined });
          }}
        />

        <label className="flex items-start gap-2 rounded-xl bg-slate-50 px-3 py-3 text-sm text-slate-600">
          <input
            type="checkbox"
            checked={form.terms}
            onChange={() => {
              setForm({ ...form, terms: !form.terms });
              if (errors.terms) setErrors({ ...errors, terms: undefined });
            }}
            className="mt-1 h-4 w-4 rounded border-slate-300 text-indigo-600 focus:ring-indigo-500"
          />
          <span>I agree to the terms and privacy notice.</span>
        </label>
        {errors.terms ? <p className="-mt-2 text-xs text-rose-600">{errors.terms}</p> : null}

        {message ? <p className="rounded-xl bg-emerald-50 px-3 py-2 text-sm text-emerald-700">{message}</p> : null}

        <Button type="submit" className="w-full">
          Create account
        </Button>
      </form>
    </AuthCard>
  );
}
