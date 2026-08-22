import React, { useState, useEffect } from 'react';
import { StyleSheet, Text, View, ActivityIndicator, SafeAreaView, StatusBar } from 'react-native';
import { RouteProp } from '@react-navigation/native';
import { StackNavigationProp } from '@react-navigation/stack';
import Constants from 'expo-constants';
import { RootStackParamList } from '../navigation/AppNavigator';
import { colors } from '../theme/colors';

type LoadingScreenRouteProp = RouteProp<RootStackParamList, 'Loading'>;

interface Props {
  route: LoadingScreenRouteProp;
  navigation: StackNavigationProp<RootStackParamList, 'Loading'>;
}

const PLANT_TIPS = [
  'Yellow leaves often mean overwatering. Make sure the soil dries out between waterings.',
  'Ensure your pot has drainage holes. Standing water can cause root rot.',
  'Brown, crispy leaf edges usually indicate low humidity or underwatering.',
  "Dust your plant's leaves occasionally so they can photosynthesize effectively.",
  'Most houseplants prefer bright, indirect sunlight rather than harsh direct sun.',
  'If you notice small webs on leaves, it might be spider mites. Mist the leaves and wipe them down.',
  'Always check the soil moisture with your finger before adding more water.',
  'Grouping plants together helps increase local humidity levels naturally.',
];

const getApiUrl = () => {
  const hostUri = Constants.expoConfig?.hostUri;
  const ip = hostUri ? hostUri.split(':')[0] : 'localhost';
  return `http://${ip}:8080/api/diagnose`;
};

const API_URL = getApiUrl();

export default function LoadingScreen({ route, navigation }: Props) {
  const { imageUri } = route.params;
  const [tipIndex, setTipIndex] = useState(0);
  const [statusMessage, setStatusMessage] = useState('Uploading image...');

  useEffect(() => {
    const tipInterval = setInterval(() => {
      setTipIndex((prev) => (prev + 1) % PLANT_TIPS.length);
    }, 6000);
    return () => clearInterval(tipInterval);
  }, []);

  useEffect(() => {
    const messages = [
      'Uploading image...',
      'Analyzing leaf patterns...',
      'Matching visible symptoms with DB...',
      'Synthesizing pathological advice...',
      'Finalizing diagnosis data...',
    ];
    let i = 0;
    const msgInterval = setInterval(() => {
      if (i < messages.length - 1) {
        i++;
        setStatusMessage(messages[i]);
      }
    }, 15000);
    return () => clearInterval(msgInterval);
  }, []);

  useEffect(() => {
    let active = true;
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 180000);

    const performDiagnosis = async () => {
      try {
        const formData = new FormData();
        const uriParts = imageUri.split('.');
        const fileType = uriParts[uriParts.length - 1];

        formData.append('image', {
          uri: imageUri,
          name: `photo.${fileType}`,
          type: `image/${fileType === 'png' ? 'png' : 'jpeg'}`,
        } as any);

        const response = await fetch(API_URL, {
          method: 'POST',
          body: formData,
          headers: {
            Accept: 'application/json',
            'Content-Type': 'multipart/form-data',
          },
          signal: controller.signal,
        });

        clearTimeout(timeoutId);
        if (!active) return;

        if (response.ok) {
          const data = await response.json();
          navigation.replace('Results', { diagnosis: data });
        } else {
          let errorText = 'Server returned an error';
          try {
            const errJson = await response.json();
            errorText = errJson.error || errorText;
          } catch (_) {
            /* ignore parse errors */
          }
          navigation.replace('Error', { message: errorText });
        }
      } catch (error: unknown) {
        clearTimeout(timeoutId);
        if (!active) return;

        console.error('Diagnosis error:', error);
        if (error instanceof Error && error.name === 'AbortError') {
          navigation.replace('Error', {
            message:
              'The diagnosis took too long. The server is not responding. Please try again.',
            isTimeout: true,
          });
        } else {
          navigation.replace('Error', {
            message:
              'Unable to connect to the server. Please check your network connection and try again.',
          });
        }
      }
    };

    performDiagnosis();

    return () => {
      active = false;
      controller.abort();
      clearTimeout(timeoutId);
    };
  }, [imageUri, navigation]);

  return (
    <SafeAreaView style={styles.safeArea}>
      <StatusBar barStyle="dark-content" backgroundColor={colors.background} />
      <View style={styles.container}>
        <View style={styles.loadingContainer}>
          <ActivityIndicator size="large" color={colors.secondary} style={styles.spinner} />
          <Text style={styles.statusTitle}>{statusMessage}</Text>
          <Text style={styles.statusSubtitle}>
            This may take up to a minute. Please don&apos;t close the app.
          </Text>
        </View>

        <View style={styles.tipCard}>
          <View style={styles.tipHeader}>
            <Text style={styles.tipIcon}>💡</Text>
            <Text style={styles.tipTitle}>Plant Care Tip</Text>
          </View>
          <Text style={styles.tipText}>{PLANT_TIPS[tipIndex]}</Text>
        </View>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: colors.background,
  },
  container: {
    flex: 1,
    paddingHorizontal: 24,
    justifyContent: 'center',
    alignItems: 'center',
    gap: 40,
  },
  loadingContainer: {
    alignItems: 'center',
  },
  spinner: {
    transform: [{ scale: 1.5 }],
    marginBottom: 24,
  },
  statusTitle: {
    fontSize: 20,
    fontWeight: '700',
    color: colors.textPrimary,
    textAlign: 'center',
  },
  statusSubtitle: {
    fontSize: 14,
    color: colors.textSecondary,
    marginTop: 8,
    textAlign: 'center',
    paddingHorizontal: 20,
  },
  tipCard: {
    backgroundColor: colors.surface,
    borderRadius: 20,
    padding: 20,
    width: '100%',
    shadowColor: colors.shadow,
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.04,
    shadowRadius: 12,
    elevation: 2,
    borderWidth: 1,
    borderColor: colors.borderLight,
  },
  tipHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 10,
    gap: 6,
  },
  tipIcon: {
    fontSize: 18,
  },
  tipTitle: {
    fontSize: 14,
    fontWeight: '700',
    color: colors.secondary,
    textTransform: 'uppercase',
    letterSpacing: 0.5,
  },
  tipText: {
    fontSize: 14,
    color: colors.textSecondary,
    lineHeight: 20,
  },
});
