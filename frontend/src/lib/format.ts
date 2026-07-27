export function relativeTime(iso: string, now: number = Date.now()): string {
  const diffMs = now - new Date(iso).getTime();
  const minutes = Math.round(diffMs / 60000);
  if (minutes < 1) return 'just now';
  if (minutes < 60) return `${minutes} min ago`;
  const hours = Math.round(minutes / 60);
  if (hours < 24) return `${hours} hr ago`;
  const days = Math.round(hours / 24);
  return `${days} d ago`;
}

export function stressTrend(averageIntensity: number): 'Low' | 'Medium' | 'High' {
  if (averageIntensity >= 7) return 'High';
  if (averageIntensity >= 4) return 'Medium';
  return 'Low';
}
