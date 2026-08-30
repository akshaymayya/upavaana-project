import React from 'react';
import { StyleProp, StyleSheet, Text, TextStyle, View } from 'react-native';
import { parseInlineBold, splitGuidanceLines } from '../utils/formatGuidance';

type Props = {
	text: string;
	style?: StyleProp<TextStyle>;
};

export default function FormattedGuidanceText({ text, style }: Props) {
	const lines = splitGuidanceLines(text);

	return (
		<View>
			{lines.map((line, lineIndex) => (
				<Text key={lineIndex} style={[styles.line, style]}>
					{parseInlineBold(line).map((span, spanIndex) => (
						<Text key={spanIndex} style={span.bold ? styles.bold : undefined}>
							{span.text}
						</Text>
					))}
				</Text>
			))}
		</View>
	);
}

const styles = StyleSheet.create({
	line: {
		marginBottom: 8,
	},
	bold: {
		fontWeight: '700',
	},
});
