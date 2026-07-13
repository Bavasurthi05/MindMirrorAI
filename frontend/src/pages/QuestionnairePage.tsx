import { useMemo, useState } from 'react';
import { motion } from 'framer-motion';
import { Button } from '../components/ui/button';

const questions = [
  {
    id: 1,
    question: 'How would you describe your energy level today?',
    options: ['Low', 'Moderate', 'High', 'Very High'],
  },
  {
    id: 2,
    question: 'How supported do you feel by the people around you?',
    options: ['Not at all', 'Somewhat', 'Mostly', 'Completely'],
  },
  {
    id: 3,
    question: 'Which best describes your focus today?',
    options: ['Distracted', 'Balanced', 'Clear', 'Highly focused'],
  },
  {
    id: 4,
    question: 'How calm or stressed do you feel right now?',
    options: ['Very stressed', 'Stressed', 'Calm', 'Very calm'],
  },
];

export function QuestionnairePage() {
  const [step, setStep] = useState(0);
  const [answers, setAnswers] = useState<string[]>(Array(questions.length).fill(''));
  const [submitted, setSubmitted] = useState(false);

  const currentQuestion = questions[step];
  const progress = useMemo(() => ((step + 1) / questions.length) * 100, [step]);

  const handleSelect = (value: string) => {
    const updated = [...answers];
    updated[step] = value;
    setAnswers(updated);
  };

  const handleNext = () => {
    if (!answers[step]) return;
    if (step < questions.length - 1) {
      setStep(step + 1);
    } else {
      setSubmitted(true);
    }
  };

  const handlePrevious = () => {
    if (step > 0) setStep(step - 1);
  };

  const handleRestart = () => {
    setStep(0);
    setAnswers(Array(questions.length).fill(''));
    setSubmitted(false);
  };

  if (submitted) {
    return (
      <motion.div
        initial={{ opacity: 0, y: 14 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.25 }}
        className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm sm:p-10"
      >
        <div className="mx-auto max-w-xl text-center">
          <div className="mx-auto mb-5 flex h-16 w-16 items-center justify-center rounded-full bg-emerald-100 text-3xl">
            ✓
          </div>
          <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600">Completion</p>
          <h1 className="mt-2 text-3xl font-semibold text-slate-900">Questionnaire complete</h1>
          <p className="mt-3 text-sm leading-7 text-slate-600">
            This mock questionnaire is complete. No backend processing or API call was used.
          </p>
          <div className="mt-6 rounded-2xl border border-slate-200 bg-slate-50 p-4 text-left text-sm text-slate-600">
            <p className="font-semibold text-slate-900">Your responses</p>
            <ul className="mt-3 space-y-2">
              {answers.map((answer, index) => (
                <li key={questions[index].id}>
                  <span className="font-medium text-slate-800">{index + 1}.</span> {answer || 'No answer selected'}
                </li>
              ))}
            </ul>
          </div>
          <Button className="mt-6" onClick={handleRestart}>
            Start again
          </Button>
        </div>
      </motion.div>
    );
  }

  return (
    <motion.div
      initial={{ opacity: 0, y: 14 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.25 }}
      className="rounded-[2rem] border border-slate-200 bg-white p-6 shadow-sm sm:p-8"
    >
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600">Mental Health Questionnaire</p>
          <h1 className="mt-2 text-3xl font-semibold text-slate-900">Check-in with yourself</h1>
        </div>
        <div className="rounded-full bg-slate-100 px-3 py-1 text-sm font-medium text-slate-700">
          Step {step + 1} of {questions.length}
        </div>
      </div>

      <div className="mt-6 h-2 rounded-full bg-slate-200">
        <div className="h-2 rounded-full bg-gradient-to-r from-cyan-500 to-indigo-500" style={{ width: `${progress}%` }} />
      </div>

      <div className="mt-8 rounded-[1.5rem] border border-slate-200 bg-slate-50 p-6">
        <p className="text-sm font-semibold text-slate-600">Question {step + 1}</p>
        <h2 className="mt-2 text-2xl font-semibold text-slate-900">{currentQuestion.question}</h2>

        <div className="mt-6 grid gap-3">
          {currentQuestion.options.map((option) => (
            <button
              key={option}
              type="button"
              onClick={() => handleSelect(option)}
              className={`rounded-2xl border px-4 py-3 text-left text-sm transition ${
                answers[step] === option
                  ? 'border-indigo-500 bg-indigo-600 text-white'
                  : 'border-slate-200 bg-white text-slate-700 hover:bg-slate-50'
              }`}
            >
              {option}
            </button>
          ))}
        </div>
      </div>

      <div className="mt-8 flex flex-wrap justify-between gap-3">
        <Button variant="secondary" onClick={handlePrevious} disabled={step === 0}>
          Previous
        </Button>

        <div className="flex gap-2">
          {step < questions.length - 1 ? (
            <Button onClick={handleNext} disabled={!answers[step]}>
              Next
            </Button>
          ) : (
            <Button onClick={handleNext} disabled={!answers[step]}>
              Submit
            </Button>
          )}
        </div>
      </div>
    </motion.div>
  );
}
