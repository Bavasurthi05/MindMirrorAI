import { Link } from 'react-router-dom';
import { AuthCard } from '../../components/auth/AuthCard';
import { Button } from '../../components/ui/button';

export function EmailVerificationPage() {
  return (
    <AuthCard
      title="Verify your email"
      description="We have prepared a confirmation experience for your account setup."
      footerText="Already verified?"
      footerLinkText="Go to sign in"
      footerHref="/login"
    >
      <div className="space-y-4 text-center">
        <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-emerald-100 text-3xl">
          ✉️
        </div>
        <div className="space-y-2">
          <p className="text-lg font-semibold text-slate-900">A confirmation message is ready to be sent.</p>
          <p className="text-sm leading-6 text-slate-600">
            This page is intentionally front-end only and can be connected to your email verification flow later.
          </p>
        </div>
        <div className="flex flex-wrap justify-center gap-3">
          <Button>Resend email</Button>
          <Link to="/login">
            <Button variant="secondary">Back to login</Button>
          </Link>
        </div>
      </div>
    </AuthCard>
  );
}
