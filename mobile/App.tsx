import 'react-native-gesture-handler';
import React from 'react';
import { NavigationContainer } from '@react-navigation/native';
import AppNavigator from './src/navigation/AppNavigator';
import { DiagnosisSessionProvider } from './src/context/DiagnosisSessionContext';

export default function App() {
  return (
    <DiagnosisSessionProvider>
      <NavigationContainer>
        <AppNavigator />
      </NavigationContainer>
    </DiagnosisSessionProvider>
  );
}

