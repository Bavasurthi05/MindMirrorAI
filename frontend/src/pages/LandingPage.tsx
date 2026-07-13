import { motion } from 'framer-motion';
import { Link } from 'react-router-dom';

const featureCards = [
  {
    title: 'Live Mood Insights',
    description: 'Track emotional shifts with gentle summaries and adaptive visual stories.',
    icon: '🌤️',
  },
  {
    title: 'AI Reflection Coach',
    description: 'Receive supportive prompts that turn journal entries into meaningful next steps.',
    icon: '🧠',
  },
  {
    title: 'Care Planning',
    description: 'Build personalized wellness plans using context from behavior, mood, and habits.',
    icon: '🛟',
  },
];

const aiCapabilities = [
  'Emotion pattern recognition',
  'Trigger and stress analysis',
  'Personalized recovery recommendations',
  'Narrative insights for reflection',
];

const steps = [
  {
    title: '1. Share your day',
    text: 'Log mood, sleep, activities, and reflections in a calm, guided experience.',
  },
  {
    title: '2. Let AI interpret',
    text: 'The platform highlights patterns and signals that may matter to your wellbeing.',
  },
  {
    title: '3. Follow your next step',
    text: 'Get thoughtful recommendations and clear progress checkpoints that feel actionable.',
  },
];

const benefits = [
  'A clearer picture of emotional trends over time',
  'Helpful support without overwhelming complexity',
  'Private, human-centered guidance built for reflection',
  'A strong foundation for care conversations and recovery planning',
];

const testimonials = [
  {
    quote: 'It feels like a calm mirror for my week — easy to understand and surprisingly insightful.',
    author: 'Mina, Student',
  },
  {
    quote: 'The summaries helped me notice patterns I was missing and make better choices.',
    author: 'Daniel, Creative Professional',
  },
];

const faqs = [
  {
    question: 'Is this platform meant for clinical diagnosis?',
    answer: 'No. It is designed to support self-reflection and wellness awareness with supportive AI insights.',
  },
  {
    question: 'How is the experience personalized?',
    answer: 'The experience learns from the patterns you share over time and adapts its guidance accordingly.',
  },
  {
    question: 'What kind of data is used?',
    answer: 'The platform uses dummy content and sample interactions in this design preview, with a focus on privacy-first structure.',
  },
];

export function LandingPage() {
  return (
    <div className="space-y-8">
      <motion.section
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.45 }}
        className="overflow-hidden rounded-[2rem] border border-slate-200 bg-gradient-to-br from-slate-950 via-slate-900 to-indigo-950 p-8 text-white shadow-xl sm:p-10 lg:p-14"
      >
        <div className="grid gap-10 lg:grid-cols-[1.1fr_0.9fr] lg:items-center">
          <div className="space-y-6">
            <div className="inline-flex rounded-full border border-white/20 bg-white/10 px-3 py-1 text-sm text-slate-200">
              AI-Powered Mental Health Analytics Platform
            </div>
            <div className="space-y-4">
              <h1 className="text-4xl font-semibold leading-tight sm:text-5xl">
                Understand your wellbeing with calm, intelligent insight.
              </h1>
              <p className="max-w-2xl text-lg text-slate-300">
                Discover how daily reflections, mood signals, and behavior patterns can be turned into a supportive digital experience.
              </p>
            </div>
            <div className="flex flex-wrap gap-3">
              <Link
                to="/register"
                className="rounded-full bg-cyan-400 px-5 py-3 font-semibold text-slate-950 transition hover:bg-cyan-300"
              >
                Start your journey
              </Link>
              <Link
                to="/login"
                className="rounded-full border border-white/20 px-5 py-3 font-semibold text-white transition hover:bg-white/10"
              >
                Explore platform
              </Link>
            </div>
            <div className="flex flex-wrap gap-6 pt-2 text-sm text-slate-300">
              <div>
                <div className="text-2xl font-semibold text-white">12k+</div>
                <div>guided reflections</div>
              </div>
              <div>
                <div className="text-2xl font-semibold text-white">94%</div>
                <div>user clarity rate</div>
              </div>
              <div>
                <div className="text-2xl font-semibold text-white">24/7</div>
                <div>gentle support flow</div>
              </div>
            </div>
          </div>

          <div className="rounded-[1.5rem] border border-white/10 bg-white/10 p-4 backdrop-blur">
            <div className="rounded-[1.25rem] bg-slate-50 p-4 text-slate-900 shadow-2xl">
              <div className="flex items-center justify-between border-b border-slate-200 pb-3">
                <div>
                  <p className="text-sm font-semibold text-slate-500">Wellness Summary</p>
                  <p className="text-xl font-semibold">Your weekly rhythm</p>
                </div>
                <div className="rounded-full bg-emerald-100 px-3 py-1 text-sm font-medium text-emerald-700">
                  Stable
                </div>
              </div>
              <div className="mt-4 space-y-4">
                <div className="rounded-2xl bg-slate-100 p-4">
                  <div className="mb-2 flex items-center justify-between text-sm text-slate-600">
                    <span>Mood trend</span>
                    <span>+18%</span>
                  </div>
                  <div className="h-2 rounded-full bg-slate-200">
                    <div className="h-2 w-4/5 rounded-full bg-cyan-500" />
                  </div>
                </div>
                <div className="grid gap-3 sm:grid-cols-2">
                  <div className="rounded-2xl bg-indigo-50 p-4">
                    <p className="text-sm text-slate-500">Sleep quality</p>
                    <p className="text-2xl font-semibold text-indigo-700">7.8/10</p>
                  </div>
                  <div className="rounded-2xl bg-amber-50 p-4">
                    <p className="text-sm text-slate-500">Stress signals</p>
                    <p className="text-2xl font-semibold text-amber-700">Low</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </motion.section>

      <section className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm sm:p-10">
        <div className="mb-8 max-w-2xl">
          <p className="text-sm font-semibold uppercase tracking-[0.2em] text-cyan-600">Features</p>
          <h2 className="mt-2 text-3xl font-semibold text-slate-900">A thoughtful experience built around clarity and care.</h2>
        </div>
        <div className="grid gap-5 md:grid-cols-3">
          {featureCards.map((card) => (
            <motion.div
              key={card.title}
              whileHover={{ y: -4, scale: 1.01 }}
              className="rounded-2xl border border-slate-200 bg-slate-50 p-6"
            >
              <div className="mb-4 text-3xl">{card.icon}</div>
              <h3 className="text-lg font-semibold text-slate-900">{card.title}</h3>
              <p className="mt-2 text-sm leading-6 text-slate-600">{card.description}</p>
            </motion.div>
          ))}
        </div>
      </section>

      <section className="grid gap-6 lg:grid-cols-[0.95fr_1.05fr]">
        <div className="rounded-[2rem] border border-slate-200 bg-slate-900 p-8 text-white shadow-sm sm:p-10">
          <p className="text-sm font-semibold uppercase tracking-[0.2em] text-cyan-300">AI Capabilities</p>
          <h2 className="mt-2 text-3xl font-semibold">From raw reflections to meaningful patterns.</h2>
          <ul className="mt-6 space-y-3 text-sm text-slate-300">
            {aiCapabilities.map((item) => (
              <li key={item} className="flex items-start gap-3">
                <span className="mt-1 text-cyan-300">✦</span>
                <span>{item}</span>
              </li>
            ))}
          </ul>
        </div>

        <div className="rounded-[2rem] border border-slate-200 bg-gradient-to-br from-cyan-50 to-indigo-50 p-8 shadow-sm sm:p-10">
          <p className="text-sm font-semibold uppercase tracking-[0.2em] text-indigo-600">Digital Mental Mirror</p>
          <h2 className="mt-2 text-3xl font-semibold text-slate-900">See your story evolve in a human-centered, supportive way.</h2>
          <p className="mt-4 text-sm leading-7 text-slate-600">
            The digital mental mirror turns scattered thoughts into a visible overview of mood, habits, and emotional context — creating a gentle space for reflection rather than pressure.
          </p>
          <div className="mt-6 rounded-2xl border border-indigo-100 bg-white/80 p-4">
            <div className="flex items-center justify-between text-sm text-slate-600">
              <span>Reflection depth</span>
              <span className="font-semibold text-slate-900">High</span>
            </div>
            <div className="mt-3 h-2 rounded-full bg-slate-200">
              <div className="h-2 w-4/5 rounded-full bg-indigo-500" />
            </div>
          </div>
        </div>
      </section>

      <section className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm sm:p-10">
        <div className="mb-8 max-w-2xl">
          <p className="text-sm font-semibold uppercase tracking-[0.2em] text-cyan-600">How it works</p>
          <h2 className="mt-2 text-3xl font-semibold text-slate-900">A simple path from reflection to insight.</h2>
        </div>
        <div className="grid gap-4 md:grid-cols-3">
          {steps.map((step) => (
            <div key={step.title} className="rounded-2xl border border-slate-200 bg-slate-50 p-6">
              <h3 className="text-lg font-semibold text-slate-900">{step.title}</h3>
              <p className="mt-2 text-sm leading-6 text-slate-600">{step.text}</p>
            </div>
          ))}
        </div>
      </section>

      <section className="grid gap-6 lg:grid-cols-[1fr_0.95fr]">
        <div className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm sm:p-10">
          <p className="text-sm font-semibold uppercase tracking-[0.2em] text-cyan-600">Benefits</p>
          <h2 className="mt-2 text-3xl font-semibold text-slate-900">Why this experience stands out.</h2>
          <ul className="mt-6 space-y-3 text-sm text-slate-600">
            {benefits.map((item) => (
              <li key={item} className="flex items-start gap-3">
                <span className="mt-1 text-emerald-500">✓</span>
                <span>{item}</span>
              </li>
            ))}
          </ul>
        </div>

        <div className="rounded-[2rem] border border-slate-200 bg-slate-50 p-8 shadow-sm sm:p-10">
          <p className="text-sm font-semibold uppercase tracking-[0.2em] text-cyan-600">Testimonials</p>
          <div className="mt-6 space-y-4">
            {testimonials.map((item) => (
              <div key={item.author} className="rounded-2xl border border-slate-200 bg-white p-5">
                <p className="text-sm leading-7 text-slate-600">“{item.quote}”</p>
                <p className="mt-3 text-sm font-semibold text-slate-900">{item.author}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm sm:p-10">
        <div className="mb-6 max-w-2xl">
          <p className="text-sm font-semibold uppercase tracking-[0.2em] text-cyan-600">FAQ</p>
          <h2 className="mt-2 text-3xl font-semibold text-slate-900">Questions you might have.</h2>
        </div>
        <div className="space-y-3">
          {faqs.map((faq) => (
            <details key={faq.question} className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
              <summary className="cursor-pointer font-semibold text-slate-900">{faq.question}</summary>
              <p className="mt-3 text-sm leading-7 text-slate-600">{faq.answer}</p>
            </details>
          ))}
        </div>
      </section>

      <footer className="rounded-[2rem] border border-slate-200 bg-slate-950 px-8 py-10 text-slate-300 shadow-sm sm:px-10">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-lg font-semibold text-white">Mindful AI Analytics</p>
            <p className="mt-1 text-sm">A modern platform experience for wellbeing awareness and reflection.</p>
          </div>
          <div className="flex gap-4 text-sm">
            <a href="/login" className="transition hover:text-white">Sign in</a>
            <a href="/register" className="transition hover:text-white">Create account</a>
          </div>
        </div>
      </footer>
    </div>
  );
}
