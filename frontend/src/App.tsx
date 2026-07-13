import { Routes, Route, Navigate } from 'react-router-dom';
import { PublicLayout } from './components/layout/PublicLayout';
import { ProtectedLayout } from './components/layout/ProtectedLayout';
import { AdminLayout } from './components/layout/AdminLayout';
import { PlaceholderPage } from './pages/PlaceholderPage';
import { LandingPage } from './pages/LandingPage';
import { LoginPage } from './pages/auth/LoginPage';
import { RegisterPage } from './pages/auth/RegisterPage';
import { ForgotPasswordPage } from './pages/auth/ForgotPasswordPage';
import { ResetPasswordPage } from './pages/auth/ResetPasswordPage';
import { EmailVerificationPage } from './pages/auth/EmailVerificationPage';
import { LoadingScreenPage } from './pages/auth/LoadingScreenPage';
import { DashboardPage } from './pages/DashboardPage';
import { JournalPage } from './pages/JournalPage';
import { QuestionnairePage } from './pages/QuestionnairePage';
import { MirrorPage } from './pages/MirrorPage';
import { TriggerAnalyticsPage } from './pages/TriggerAnalyticsPage';
import { RecoveryPlanPage } from './pages/RecoveryPlanPage';
import { MoodTimelinePage } from './pages/MoodTimelinePage';
import { ReportPreviewPage } from './pages/ReportPreviewPage';
import { AdminDashboardPage } from './pages/AdminDashboardPage';

function App() {
  return (
    <Routes>
      <Route element={<PublicLayout />}>
        <Route path="/" element={<LandingPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/forgot-password" element={<ForgotPasswordPage />} />
        <Route path="/reset-password" element={<ResetPasswordPage />} />
        <Route path="/verify-email" element={<EmailVerificationPage />} />
        <Route path="/loading" element={<LoadingScreenPage />} />
      </Route>

      <Route element={<ProtectedLayout />}>
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/journal" element={<JournalPage />} />
        <Route path="/questionnaire" element={<QuestionnairePage />} />
        <Route path="/mirror" element={<MirrorPage />} />
        <Route path="/prediction-results" element={<PlaceholderPage title="Prediction Results" description="See AI-driven analysis outcomes." />} />
        <Route path="/trigger-analytics" element={<TriggerAnalyticsPage />} />
        <Route path="/mood-timeline" element={<MoodTimelinePage />} />
        <Route path="/weekly-insights" element={<PlaceholderPage title="Weekly Insights" description="View AI-driven weekly wellness summaries." />} />
        <Route path="/recommendations" element={<RecoveryPlanPage />} />
        <Route path="/reports" element={<ReportPreviewPage />} />
        <Route path="/profile" element={<PlaceholderPage title="Profile" description="Manage user details and preferences." />} />
        <Route path="/settings" element={<PlaceholderPage title="Settings" description="Adjust application preferences and privacy controls." />} />
      </Route>

      <Route element={<AdminLayout />}>
        <Route path="/admin" element={<AdminDashboardPage />} />
        <Route path="/admin/analytics" element={<PlaceholderPage title="Admin Analytics" description="Monitor trends, usage, and system health." />} />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default App;
