import React from 'react';
import { StyleSheet, Text, View, TouchableOpacity, SafeAreaView, StatusBar } from 'react-native';
import { RouteProp } from '@react-navigation/native';
import { StackNavigationProp } from '@react-navigation/stack';
import { RootStackParamList } from '../navigation/AppNavigator';
import { colors } from '../theme/colors';

type ErrorScreenRouteProp = RouteProp<RootStackParamList, 'Error'>;
type ErrorScreenNavigationProp = StackNavigationProp<RootStackParamList, 'Error'>;

interface Props {
  route: ErrorScreenRouteProp;
  navigation: ErrorScreenNavigationProp;
}

export default function ErrorScreen({ route, navigation }: Props) {
  const { message, isTimeout } = route?.params || {};

  const handleRetry = () => {
    navigation.popToTop();
  };

  let title = 'Diagnosis Failed';
  let displayMessage = 'Something went wrong while analyzing your plant. Please try again.';
  let tip = 'Make sure the photo is clear, well-lit, and focuses on the leaf or stem of the plant.';

  if (isTimeout) {
    title = 'Request Timed Out';
    displayMessage = 'This is taking longer than expected. The server might be busy or unreachable.';
    tip =
      'Please check your network signal and try again. We have added retries, but transient slowness can still happen.';
  } else if (message && message.toLowerCase().includes('connection')) {
    title = 'Network Connection Error';
    displayMessage = "We couldn't connect to the server.";
    tip =
      "Make sure your mobile device has internet access and can reach the server's network address.";
  }

  return (
    <SafeAreaView style={styles.safeArea}>
      <StatusBar barStyle="dark-content" backgroundColor={colors.errorBackground} />
      <View style={styles.container}>
        <View style={styles.contentContainer}>
          <View style={styles.errorIconCircle}>
            <Text style={styles.errorIcon}>🥀</Text>
          </View>
          <Text style={styles.errorTitle}>{title}</Text>
          <Text style={styles.errorMessage}>{displayMessage}</Text>

          <View style={styles.tipCard}>
            <Text style={styles.tipTitle}>💡 TIP FOR SUCCESS</Text>
            <Text style={styles.tipText}>{tip}</Text>
          </View>
        </View>

        <View style={styles.buttonContainer}>
          <TouchableOpacity style={styles.retryButton} onPress={handleRetry} activeOpacity={0.85}>
            <Text style={styles.retryButtonText}>Try Again</Text>
          </TouchableOpacity>
        </View>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: colors.errorBackground,
  },
  container: {
    flex: 1,
    paddingHorizontal: 24,
    justifyContent: 'space-between',
    paddingVertical: 32,
  },
  contentContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    marginTop: 40,
  },
  errorIconCircle: {
    width: 90,
    height: 90,
    borderRadius: 45,
    backgroundColor: colors.errorSurface,
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 24,
  },
  errorIcon: {
    fontSize: 44,
  },
  errorTitle: {
    fontSize: 24,
    fontWeight: '800',
    color: colors.error,
    textAlign: 'center',
    marginBottom: 10,
  },
  errorMessage: {
    fontSize: 15,
    color: colors.textSecondary,
    textAlign: 'center',
    lineHeight: 22,
    paddingHorizontal: 20,
    marginBottom: 32,
  },
  tipCard: {
    backgroundColor: colors.surface,
    borderRadius: 20,
    padding: 20,
    width: '100%',
    shadowColor: colors.error,
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.03,
    shadowRadius: 12,
    elevation: 1,
    borderWidth: 1,
    borderColor: colors.errorBorder,
  },
  tipTitle: {
    fontSize: 11,
    fontWeight: '700',
    color: colors.error,
    letterSpacing: 0.5,
    marginBottom: 8,
  },
  tipText: {
    fontSize: 13,
    color: colors.textSecondary,
    lineHeight: 18,
  },
  buttonContainer: {
    width: '100%',
  },
  retryButton: {
    backgroundColor: colors.cta,
    paddingVertical: 18,
    borderRadius: 16,
    alignItems: 'center',
    justifyContent: 'center',
    shadowColor: colors.cta,
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.25,
    shadowRadius: 8,
    elevation: 3,
  },
  retryButtonText: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '700',
  },
});
