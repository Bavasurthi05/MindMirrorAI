import { Routes, Route, Navigate } from 'react-router-dom';
import { PublicLayout } from './components/layout/PublicLayout';
import { ProtectedLayout } from './components/layout/ProtectedLayout';
import { AdminLayout } from './components/layout/AdminLayout';
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
import { PredictionResultsPage } from './pages/PredictionResultsPage';
import { SocialMediaAnalysisPage } from './pages/SocialMediaAnalysisPage';
import { AnalyticsPage } from './pages/AnalyticsPage';
import { WeeklyInsightsPage } from './pages/WeeklyInsightsPage';
import { EmergencyHelpPage } from './pages/EmergencyHelpPage';
import { FeedbackPage } from './pages/FeedbackPage';
import { ProfilePage } from './pages/ProfilePage';
import { SettingsPage } from './pages/SettingsPage';
import { AdminAnalyticsPage } from './pages/AdminAnalyticsPage';

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
        <Route path="/social-analysis" element={<SocialMediaAnalysisPage />} />
        <Route path="/mirror" element={<MirrorPage />} />
        <Route path="/prediction-results" element={<PredictionResultsPage />} />
        <Route path="/trigger-analytics" element={<TriggerAnalyticsPage />} />
        <Route path="/mood-timeline" element={<MoodTimelinePage />} />
        <Route path="/analytics" element={<AnalyticsPage />} />
        <Route path="/weekly-insights" element={<WeeklyInsightsPage />} />
        <Route path="/recommendations" element={<RecoveryPlanPage />} />
        <Route path="/reports" element={<ReportPreviewPage />} />
        <Route path="/help" element={<EmergencyHelpPage />} />
        <Route path="/feedback" element={<FeedbackPage />} />
        <Route path="/profile" element={<ProfilePage />} />
        <Route path="/settings" element={<SettingsPage />} />
      </Route>

      <Route element={<AdminLayout />}>
        <Route path="/admin" element={<AdminDashboardPage />} />
        <Route path="/admin/analytics" element={<AdminAnalyticsPage />} />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default App;
