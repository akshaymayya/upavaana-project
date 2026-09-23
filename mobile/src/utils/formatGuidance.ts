export type GuidanceSpan = {
	text: string;
	bold: boolean;
};

/** Split on newlines; keep empty lines so the renderer can add vertical gaps. */
export function splitGuidanceLines(text: string): string[] {
	if (text == null || text === '') {
		return [''];
	}
	const normalized = text.replace(/\r\n/g, '\n').replace(/\\n/g, '\n');
	return normalized.split('\n');
}

/**
 * Markdown-lite: `**phrase**` → bold. Unmatched `**` stays as plain text.
 */
export function parseInlineBold(line: string): GuidanceSpan[] {
	const spans: GuidanceSpan[] = [];
	const re = /\*\*(.+?)\*\*/g;
	let last = 0;
	let match: RegExpExecArray | null;
	while ((match = re.exec(line)) !== null) {
		if (match.index > last) {
			spans.push({ text: line.slice(last, match.index), bold: false });
		}
		spans.push({ text: match[1], bold: true });
		last = match.index + match[0].length;
	}
	if (last < line.length) {
		spans.push({ text: line.slice(last), bold: false });
	}
	if (spans.length === 0) {
		return [{ text: line, bold: false }];
	}
	return spans;
}
