import React from 'react';
import { createStackNavigator } from '@react-navigation/stack';
import HomeScreen from '../screens/HomeScreen';
import LoadingScreen from '../screens/LoadingScreen';
import ResultsScreen from '../screens/ResultsScreen';
import ErrorScreen from '../screens/ErrorScreen';
import { colors } from '../theme/colors';
import { DiagnosisData } from '../types/diagnosis';

export type { DiagnosisData };

export type RootStackParamList = {
  Home: undefined;
  Loading: { imageUri: string };
  Results: { diagnosis: DiagnosisData; sessionId?: string };
  Error: { message?: string; isTimeout?: boolean };
};

const Stack = createStackNavigator<RootStackParamList>();

export default function AppNavigator() {
  return (
    <Stack.Navigator
      initialRouteName="Home"
      screenOptions={{
        headerShown: false,
        cardStyle: { backgroundColor: colors.background },
      }}
    >
      <Stack.Screen name="Home" component={HomeScreen} />
      <Stack.Screen name="Loading" component={LoadingScreen} />
      <Stack.Screen
        name="Results"
        component={ResultsScreen}
        getId={({ params }) => params.sessionId ?? 'latest'}
      />
      <Stack.Screen name="Error" component={ErrorScreen} />
    </Stack.Navigator>
  );
}
