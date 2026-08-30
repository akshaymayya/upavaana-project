import React from 'react';
import {
  StyleSheet,
  Text,
  View,
  ScrollView,
  TouchableOpacity,
  SafeAreaView,
  StatusBar,
  Image,
} from 'react-native';
import { RouteProp } from '@react-navigation/native';
import { StackNavigationProp } from '@react-navigation/stack';
import { RootStackParamList } from '../navigation/AppNavigator';
import FormattedGuidanceText from '../components/FormattedGuidanceText';
import { colors } from '../theme/colors';

type ResultsScreenRouteProp = RouteProp<RootStackParamList, 'Results'>;
type ResultsScreenNavigationProp = StackNavigationProp<RootStackParamList, 'Results'>;

interface Props {
  route: ResultsScreenRouteProp;
  navigation: ResultsScreenNavigationProp;
}

export default function ResultsScreen({ route, navigation }: Props) {
  const { diagnosis } = route.params;

  const disease = diagnosis.disease_name?.toLowerCase() ?? '';
  const isHealthy =
    diagnosis.is_healthy === true ||
    disease.includes('healthy') ||
    disease.includes('no issues') ||
    disease.includes('no disease');

  const handleReset = () => {
    navigation.popToTop();
  };

  return (
    <SafeAreaView style={styles.safeArea}>
      <StatusBar barStyle="dark-content" backgroundColor={colors.background} />
      <View style={styles.container}>
        <ScrollView contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false}>
          <Image
            source={require('../../assets/upavana/logo-color.png')}
            style={styles.logo}
            resizeMode="contain"
            accessibilityLabel="Upavana"
          />

          <View style={[styles.statusCard, isHealthy ? styles.statusCardHealthy : styles.statusCardSick]}>
            <Text style={styles.statusEmoji}>{isHealthy ? '🎉' : '⚠️'}</Text>
            <Text style={[styles.statusTitle, isHealthy ? styles.statusTextHealthy : styles.statusTextSick]}>
              {isHealthy ? 'Plant looks Healthy!' : 'Issue Detected'}
            </Text>
          </View>

          <View style={styles.infoCard}>
            <Text style={styles.label}>IDENTIFIED PLANT</Text>
            <Text style={styles.plantName}>{diagnosis.plant_name || 'Unknown Plant'}</Text>
          </View>

          <View style={styles.infoCard}>
            <Text style={styles.label}>DIAGNOSIS</Text>
            <Text style={[styles.diseaseName, isHealthy ? styles.diseaseHealthy : styles.diseaseSick]}>
              {diagnosis.disease_name}
            </Text>
          </View>

          {diagnosis.symptoms_matched ? (
            <View style={styles.infoCard}>
              <Text style={styles.label}>SYMPTOMS OBSERVED</Text>
              <Text style={styles.bodyText}>{diagnosis.symptoms_matched}</Text>
            </View>
          ) : null}

          <View style={styles.infoCard}>
            <Text style={styles.label}>RECOMMENDED ACTION</Text>
            <FormattedGuidanceText
              text={(diagnosis.solution || '').trim() || 'No specific treatment suggested.'}
              style={styles.bodyText}
            />
          </View>

          {diagnosis.confidence_note ? (
            <View style={styles.confidenceContainer}>
              <Text style={styles.confidenceText}>🛡️ {diagnosis.confidence_note}</Text>
            </View>
          ) : null}

          <Text style={styles.disclaimer}>
            For informational guidance only — not professional horticultural or agricultural advice.
          </Text>
        </ScrollView>

        <View style={styles.buttonContainer}>
          <TouchableOpacity style={styles.resetButton} onPress={handleReset} activeOpacity={0.85}>
            <Text style={styles.resetButtonText}>Diagnose Another Plant</Text>
          </TouchableOpacity>
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
  },
  scrollContent: {
    padding: 24,
    paddingBottom: 40,
    gap: 16,
    alignItems: 'stretch',
  },
  logo: {
    width: 200,
    height: 52,
    alignSelf: 'center',
    marginBottom: 4,
  },
  statusCard: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 16,
    borderRadius: 16,
    gap: 12,
    marginBottom: 8,
  },
  statusCardHealthy: {
    backgroundColor: colors.healthyBackground,
    borderWidth: 1,
    borderColor: colors.healthyBorder,
  },
  statusCardSick: {
    backgroundColor: colors.warningBackground,
    borderWidth: 1,
    borderColor: colors.warningBorder,
  },
  statusEmoji: {
    fontSize: 24,
  },
  statusTitle: {
    fontSize: 16,
    fontWeight: '700',
  },
  statusTextHealthy: {
    color: colors.healthy,
  },
  statusTextSick: {
    color: colors.warningText,
  },
  infoCard: {
    backgroundColor: colors.surface,
    borderRadius: 20,
    padding: 20,
    shadowColor: colors.shadow,
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.04,
    shadowRadius: 12,
    elevation: 1.5,
    borderWidth: 1,
    borderColor: colors.borderLight,
  },
  label: {
    fontSize: 11,
    fontWeight: '700',
    color: colors.textMuted,
    letterSpacing: 1,
    marginBottom: 6,
  },
  plantName: {
    fontSize: 22,
    fontWeight: '800',
    color: colors.textPrimary,
  },
  diseaseName: {
    fontSize: 18,
    fontWeight: '700',
  },
  diseaseHealthy: {
    color: colors.healthy,
  },
  diseaseSick: {
    color: colors.error,
  },
  bodyText: {
    fontSize: 14,
    color: colors.textSecondary,
    lineHeight: 22,
  },
  confidenceContainer: {
    backgroundColor: colors.confidenceBackground,
    borderRadius: 12,
    padding: 12,
    borderWidth: 1,
    borderColor: colors.border,
  },
  confidenceText: {
    fontSize: 12,
    color: colors.textSecondary,
    lineHeight: 18,
    fontStyle: 'italic',
  },
  disclaimer: {
    fontSize: 11,
    color: colors.textMuted,
    textAlign: 'center',
    lineHeight: 16,
    marginTop: 4,
    paddingHorizontal: 8,
  },
  buttonContainer: {
    padding: 24,
    backgroundColor: colors.background,
    borderTopWidth: 1,
    borderColor: colors.borderLight,
  },
  resetButton: {
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
  resetButtonText: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '700',
  },
});
