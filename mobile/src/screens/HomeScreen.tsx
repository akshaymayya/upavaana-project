import React from 'react';
import {
  StyleSheet,
  Text,
  View,
  TouchableOpacity,
  SafeAreaView,
  StatusBar,
  Image,
} from 'react-native';
import { StackNavigationProp } from '@react-navigation/stack';
import { RootStackParamList } from '../navigation/AppNavigator';
import * as ImagePicker from 'expo-image-picker';
import { colors } from '../theme/colors';

type HomeScreenNavigationProp = StackNavigationProp<RootStackParamList, 'Home'>;

interface Props {
  navigation: HomeScreenNavigationProp;
}

export default function HomeScreen({ navigation }: Props) {
  const pickImage = async () => {
    const permissionResult = await ImagePicker.requestMediaLibraryPermissionsAsync();
    if (!permissionResult.granted) {
      alert('Permission to access the media library is required to select images.');
      return;
    }

    try {
      const result = await ImagePicker.launchImageLibraryAsync({
        mediaTypes: ImagePicker.MediaTypeOptions.Images,
        allowsEditing: true,
        quality: 0.8,
      });

      if (!result.canceled && result.assets && result.assets.length > 0) {
        navigation.navigate('Loading', { imageUri: result.assets[0].uri });
      }
    } catch (error) {
      console.error('Error picking image from library:', error);
      alert('Failed to pick image. Please try again.');
    }
  };

  const takePhoto = async () => {
    const permissionResult = await ImagePicker.requestCameraPermissionsAsync();
    if (!permissionResult.granted) {
      alert('Permission to access the camera is required to take photos.');
      return;
    }

    try {
      const result = await ImagePicker.launchCameraAsync({
        allowsEditing: true,
        quality: 0.8,
      });

      if (!result.canceled && result.assets && result.assets.length > 0) {
        navigation.navigate('Loading', { imageUri: result.assets[0].uri });
      }
    } catch (error) {
      console.error('Error taking photo:', error);
      alert('Failed to capture photo. Please try again.');
    }
  };

  return (
    <SafeAreaView style={styles.safeArea}>
      <StatusBar barStyle="dark-content" backgroundColor={colors.background} />
      <View style={styles.container}>
        <View style={styles.headerContainer}>
          <Image
            source={require('../../assets/upavana/logo-color.png')}
            style={styles.logo}
            resizeMode="contain"
            accessibilityLabel="Upavana"
          />
          <Text style={styles.brandSubtitle}>Plant Health Diagnosis</Text>
        </View>

        <View style={styles.illustrationCard}>
          <View style={styles.illustrationCircle}>
            <Text style={styles.illustrationEmoji}>🌿</Text>
          </View>
          <Text style={styles.illustrationText}>
            Keep your plants healthy and thriving. Take a clear photo of the affected leaves to
            diagnose the issue in seconds.
          </Text>
        </View>

        <View style={styles.actionsContainer}>
          <TouchableOpacity style={styles.primaryButton} onPress={takePhoto} activeOpacity={0.85}>
            <Text style={styles.primaryButtonText}>📸 Take Photo</Text>
          </TouchableOpacity>

          <TouchableOpacity style={styles.secondaryButton} onPress={pickImage} activeOpacity={0.85}>
            <Text style={styles.secondaryButtonText}>🖼️ Choose from Gallery</Text>
          </TouchableOpacity>
        </View>

        <View style={styles.footerContainer}>
          <Text style={styles.footerText}>Nature&apos;s Embrace</Text>
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
    justifyContent: 'space-between',
    paddingVertical: 32,
  },
  headerContainer: {
    alignItems: 'center',
    marginTop: 24,
  },
  logo: {
    width: 280,
    height: 72,
    marginBottom: 8,
  },
  brandSubtitle: {
    fontSize: 15,
    color: colors.textSecondary,
    marginTop: 4,
    fontWeight: '500',
  },
  illustrationCard: {
    backgroundColor: colors.surface,
    borderRadius: 24,
    padding: 24,
    alignItems: 'center',
    shadowColor: colors.shadow,
    shadowOffset: { width: 0, height: 6 },
    shadowOpacity: 0.06,
    shadowRadius: 16,
    elevation: 2,
    marginVertical: 40,
    borderWidth: 1,
    borderColor: colors.borderLight,
  },
  illustrationCircle: {
    width: 90,
    height: 90,
    borderRadius: 45,
    backgroundColor: colors.secondaryLight,
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 20,
  },
  illustrationEmoji: {
    fontSize: 44,
  },
  illustrationText: {
    fontSize: 15,
    color: colors.textSecondary,
    textAlign: 'center',
    lineHeight: 22,
    fontWeight: '400',
  },
  actionsContainer: {
    width: '100%',
    gap: 14,
  },
  primaryButton: {
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
  primaryButtonText: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '700',
  },
  secondaryButton: {
    backgroundColor: 'transparent',
    borderWidth: 2,
    borderColor: colors.primary,
    paddingVertical: 16,
    borderRadius: 16,
    alignItems: 'center',
    justifyContent: 'center',
  },
  secondaryButtonText: {
    color: colors.primary,
    fontSize: 16,
    fontWeight: '700',
  },
  footerContainer: {
    alignItems: 'center',
    marginTop: 20,
  },
  footerText: {
    fontSize: 11,
    color: colors.textMuted,
    fontWeight: '600',
    textTransform: 'uppercase',
    letterSpacing: 1.2,
  },
});
