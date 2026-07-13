import { type ReactNode } from 'react';
import { motion } from 'framer-motion';
import { Link } from 'react-router-dom';
import { Card } from '../ui/card';

interface AuthCardProps {
  title: string;
  description: string;
  children: ReactNode;
  footerText: string;
  footerLinkText: string;
  footerHref: string;
}

export function AuthCard({
  title,
  description,
  children,
  footerText,
  footerLinkText,
  footerHref,
}: AuthCardProps) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 18 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3 }}
      className="mx-auto w-full max-w-xl"
    >
      <div className="mb-6 text-center">
        <div className="mb-3 inline-flex rounded-full border border-cyan-200 bg-cyan-50 px-3 py-1 text-sm font-medium text-cyan-700">
          Secure access preview
        </div>
        <h1 className="text-3xl font-semibold text-slate-900">{title}</h1>
        <p className="mt-2 text-sm leading-6 text-slate-600">{description}</p>
      </div>

      <Card className="p-6 shadow-lg sm:p-8">
        {children}
      </Card>

      <p className="mt-5 text-center text-sm text-slate-600">
        {footerText}{' '}
        <Link to={footerHref} className="font-semibold text-indigo-600 hover:text-indigo-700">
          {footerLinkText}
        </Link>
      </p>
    </motion.div>
  );
}
