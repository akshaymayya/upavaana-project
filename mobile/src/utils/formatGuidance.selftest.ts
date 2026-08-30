import { parseInlineBold, splitGuidanceLines } from './formatGuidance.ts';

function assert(cond: unknown, msg: string): void {
	if (!cond) {
		throw new Error(msg);
	}
}

function run(): void {
	const lines = splitGuidanceLines('Lead sentence.\n**neem oil spray** on damaged leaves.\nIsolate the plant.');
	assert(lines.length === 3, 'expected 3 lines');
	const escaped = splitGuidanceLines('Step one.\\n**neem** two.');
	assert(escaped.length === 2, 'literal backslash-n should split');

	const bold = parseInlineBold('Use **neem oil spray** this week.');
	assert(bold.length === 3, 'expected 3 spans');
	assert(bold[0].text === 'Use ' && !bold[0].bold, 'prefix');
	assert(bold[1].text === 'neem oil spray' && bold[1].bold, 'bold phrase');
	assert(bold[2].text === ' this week.' && !bold[2].bold, 'suffix');

	const unmatched = parseInlineBold('Use **neem oil spray without close');
	assert(unmatched.length === 1 && unmatched[0].text.includes('**') && !unmatched[0].bold, 'unmatched stars stay plain');

	console.log('formatGuidance tests passed');
}

run();
