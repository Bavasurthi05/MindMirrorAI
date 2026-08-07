import { useMemo, useState } from 'react';
import { motion } from 'framer-motion';
import { Button } from '../components/ui/button';
import { useConnectSocialAccount, useDisconnectSocialAccount, useImportSocialContent, useSocialAccounts } from '../lib/social-accounts';
import { ApiError } from '../lib/api';

const providers = [
  { value: 'instagram', label: 'Instagram' },
  { value: 'x', label: 'X / Twitter' },
];

export function SocialAccountsPage() {
  const socialAccounts = useSocialAccounts();
  const connectSocialAccount = useConnectSocialAccount();
  const disconnectSocialAccount = useDisconnectSocialAccount();
  const importSocialContent = useImportSocialContent();
  const [provider, setProvider] = useState('instagram');
  const [externalAccountId, setExternalAccountId] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [importText, setImportText] = useState('');
  const [importedAnalysis, setImportedAnalysis] = useState<Awaited<ReturnType<typeof importSocialContent.mutateAsync>> | null>(null);

  const connectedCount = useMemo(() => socialAccounts.data?.filter((account) => account.status === 'CONNECTED').length ?? 0, [socialAccounts.data]);

  const handleConnect = async () => {
    setError('');
    setSuccess('');
    if (!externalAccountId.trim()) {
      setError('Please enter an account identifier.');
      return;
    }

    try {
      await connectSocialAccount.mutateAsync({
        provider,
        externalAccountId: externalAccountId.trim(),
        displayName: displayName.trim() || undefined,
      });
      setSuccess('Social account connected successfully.');
      setExternalAccountId('');
      setDisplayName('');
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Unable to connect the account right now.');
    }
  };

  const handleDisconnect = async (accountId: number) => {
    setError('');
    setSuccess('');
    try {
      await disconnectSocialAccount.mutateAsync(accountId);
      setSuccess('The account was disconnected.');
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Unable to disconnect the account.');
    }
  };

  const handleImport = async () => {
    setError('');
    setSuccess('');
    if (!importText.trim()) {
      setError('Paste some post content to import for analysis.');
      return;
    }

    try {
      const result = await importSocialContent.mutateAsync({ provider, content: importText.trim() });
      setImportedAnalysis(result);
      setSuccess('Imported content was analyzed successfully.');
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Unable to import and analyze the content.');
    }
  };

  return (
    <div className="space-y-6">
      <motion.section
        initial={{ opacity: 0, y: 14 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.25 }}
        className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm dark:border-slate-800 dark:bg-slate-900"
      >
        <p className="text-sm font-semibold uppercase tracking-[0.25em] text-cyan-600">Social Accounts</p>
        <h1 className="mt-2 text-3xl font-semibold text-slate-900 dark:text-white">Connect your social channels</h1>
        <p className="mt-3 max-w-3xl text-sm leading-7 text-slate-600 dark:text-slate-400">
          Link accounts so MindMirror AI can later import and analyze posts for richer context around your wellbeing.
        </p>

        <div className="mt-6 grid gap-4 lg:grid-cols-[1.2fr,0.8fr]">
          <div className="rounded-2xl border border-slate-200 bg-slate-50 p-5 dark:border-slate-700 dark:bg-slate-800">
            <p className="text-sm font-semibold text-slate-900 dark:text-white">Connected accounts</p>
            <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">
              {connectedCount > 0 ? `${connectedCount} connected` : 'No accounts linked yet'}
            </p>
            <div className="mt-4 space-y-3">
              {socialAccounts.isPending ? <p className="text-sm text-slate-500">Loading…</p> : null}
              {socialAccounts.data?.length === 0 ? (
                <p className="text-sm text-slate-500">Connect your first account to start importing posts.</p>
              ) : null}
              {socialAccounts.data?.map((account) => (
                <div key={account.id} className="flex items-center justify-between rounded-2xl border border-slate-200 bg-white p-4 dark:border-slate-700 dark:bg-slate-900">
                  <div>
                    <p className="text-sm font-semibold text-slate-900 dark:text-white">{account.displayName || account.provider}</p>
                    <p className="text-xs text-slate-500">{account.provider} • {account.status}</p>
                  </div>
                  <Button type="button" variant="secondary" onClick={() => handleDisconnect(account.id)} disabled={disconnectSocialAccount.isPending}>
                    {disconnectSocialAccount.isPending ? 'Working…' : 'Disconnect'}
                  </Button>
                </div>
              ))}
            </div>
          </div>

          <div className="space-y-4">
            <div className="rounded-2xl border border-slate-200 bg-slate-50 p-5 dark:border-slate-700 dark:bg-slate-800">
              <p className="text-sm font-semibold text-slate-900 dark:text-white">Connect an account</p>
              <label className="mt-4 block text-sm text-slate-600 dark:text-slate-400">
                Provider
                <select value={provider} onChange={(event) => setProvider(event.target.value)} className="mt-2 w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm outline-none">
                  {providers.map((item) => (
                    <option key={item.value} value={item.value}>{item.label}</option>
                  ))}
                </select>
              </label>
              <label className="mt-4 block text-sm text-slate-600 dark:text-slate-400">
                Account ID
                <input value={externalAccountId} onChange={(event) => setExternalAccountId(event.target.value)} className="mt-2 w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm outline-none" placeholder="e.g. user-123" />
              </label>
              <label className="mt-4 block text-sm text-slate-600 dark:text-slate-400">
                Display name (optional)
                <input value={displayName} onChange={(event) => setDisplayName(event.target.value)} className="mt-2 w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm outline-none" placeholder="Your public label" />
              </label>
              <Button type="button" className="mt-5" onClick={handleConnect} disabled={connectSocialAccount.isPending}>
                {connectSocialAccount.isPending ? 'Connecting…' : 'Connect account'}
              </Button>
            </div>

            <div className="rounded-2xl border border-slate-200 bg-slate-50 p-5 dark:border-slate-700 dark:bg-slate-800">
              <p className="text-sm font-semibold text-slate-900 dark:text-white">Import a post for analysis</p>
              <textarea
                value={importText}
                onChange={(event) => setImportText(event.target.value)}
                rows={4}
                className="mt-3 w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm outline-none"
                placeholder="Paste a recent post or message you want analyzed…"
              />
              <Button type="button" className="mt-4" onClick={handleImport} disabled={importSocialContent.isPending}>
                {importSocialContent.isPending ? 'Analyzing…' : 'Analyze imported content'}
              </Button>
              {importedAnalysis ? (
                <div className="mt-4 rounded-2xl border border-cyan-100 bg-cyan-50 p-4 text-sm text-slate-700">
                  <p className="font-semibold text-cyan-800">Latest analysis</p>
                  <p className="mt-1">{importedAnalysis.prediction} • {importedAnalysis.sentiment}</p>
                  <p className="mt-1 text-xs text-slate-500">Confidence {(importedAnalysis.predictionConfidence * 100).toFixed(0)}%</p>
                </div>
              ) : null}
            </div>
          </div>
        </div>

        {error ? <p className="text-sm text-rose-600">{error}</p> : null}
        {success ? <p className="text-sm text-emerald-600">{success}</p> : null}
      </motion.section>
    </div>
  );
}
