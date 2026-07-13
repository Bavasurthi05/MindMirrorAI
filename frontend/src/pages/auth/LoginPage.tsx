import { useState } from 'react';
import { Link } from 'react-router-dom';
import { AuthCard } from '../../components/auth/AuthCard';
import { Button } from '../../components/ui/button';
import { Input } from '../../components/ui/input';

interface LoginErrors {
  email?: string;
  password?: string;
}

export function LoginPage() {
  const [form, setForm] = useState({ email: '', password: '', rememberMe: false });
  const [errors, setErrors] = useState<LoginErrors>({});
  const [message, setMessage] = useState('');

  const validate = () => {
    const nextErrors: LoginErrors = {};

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

    setErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  };

  const handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setMessage('');

    if (validate()) {
      setMessage('Front-end validation passed. Authentication API can be connected later.');
    }
  };

  return (
    <AuthCard
      title="Welcome back"
      description="Sign in to continue your support journey with a calm, guided experience."
      footerText="New here?"
      footerLinkText="Create an account"
      footerHref="/register"
    >
      <form className="space-y-4" onSubmit={handleSubmit}>
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
          placeholder="Enter your password"
          value={form.password}
          error={errors.password}
          onChange={(event) => {
            setForm({ ...form, password: event.target.value });
            if (errors.password) setErrors({ ...errors, password: undefined });
          }}
        />

        <div className="flex items-center justify-between gap-3 rounded-xl bg-slate-50 px-3 py-2">
          <label className="flex items-center gap-2 text-sm text-slate-600">
            <input
              type="checkbox"
              checked={form.rememberMe}
              onChange={() => setForm({ ...form, rememberMe: !form.rememberMe })}
              className="h-4 w-4 rounded border-slate-300 text-indigo-600 focus:ring-indigo-500"
            />
            Remember me
          </label>
          <Link to="/forgot-password" className="text-sm font-medium text-indigo-600 hover:text-indigo-700">
            Forgot password?
          </Link>
        </div>

        {message ? <p className="rounded-xl bg-emerald-50 px-3 py-2 text-sm text-emerald-700">{message}</p> : null}

        <Button type="submit" className="w-full">
          Sign in
        </Button>
      </form>
    </AuthCard>
  );
}
