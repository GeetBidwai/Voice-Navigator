import React, { useEffect, useMemo, useState } from 'react';
import {
  ActivityIndicator,
  SafeAreaView,
  StatusBar,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import { InlineSettings } from './src/components/InlineSettings';
import { MicrophoneButton } from './src/components/MicrophoneButton';
import { LocationTrackerService } from './src/modules/location/locationTracker';
import { NavigationEngineService } from './src/modules/navigation/navigationEngine';
import { parseVoiceCommand } from './src/modules/parser/commandParser';
import { OpenRouteService } from './src/modules/routing/openRouteService';
import { SpeechOutputService } from './src/modules/voice/speechOutput';
import { VoiceInputService } from './src/modules/voice/voiceInput';
import { COLORS } from './src/theme/colors';
import { GeoPoint, VoiceNavState } from './src/types/navigation';

export default function App() {
  const orsService = useMemo(() => new OpenRouteService(), []);
  const speechService = useMemo(() => new SpeechOutputService(), []);
  const voiceService = useMemo(() => new VoiceInputService(true), []);
  const locationService = useMemo(() => new LocationTrackerService(), []);
  const navEngine = useMemo(
    () => new NavigationEngineService(orsService, speechService),
    [orsService, speechService]
  );

  const [state, setState] = useState<VoiceNavState>({
    instructionText: 'Where would you like to go?',
    youSaidText: '',
    isListening: false,
    isSimulatedListening: true,
    isSettingsOpen: false,
    apiKey: '',
    isApiKeyConfigured: false,
    isGpsEnabled: false,
    currentLocation: null,
    isRouting: false,
    session: navEngine.getSession(),
  });

  // Load stored API key and initiate location permission
  useEffect(() => {
    (async () => {
      const storedKey = await orsService.getStoredApiKey();
      setState((prev) => ({
        ...prev,
        apiKey: storedKey,
        isApiKeyConfigured: !!storedKey,
      }));

      const loc = await locationService.getCurrentLocation();
      if (loc) {
        setState((prev) => ({
          ...prev,
          currentLocation: loc,
          isGpsEnabled: true,
        }));
      }

      locationService.startTracking((point: GeoPoint) => {
        setState((prev) => ({
          ...prev,
          currentLocation: point,
          isGpsEnabled: true,
        }));
      });
    })();

    return () => {
      locationService.stopTracking();
      voiceService.stopListening();
    };
  }, [orsService, locationService, voiceService]);

  const handleAskPress = () => {
    if (state.isListening) {
      voiceService.stopListening();
      setState((prev) => ({ ...prev, isListening: false }));
      return;
    }

    voiceService.startListening(
      {
        onListeningStart: (isSim) => {
          setState((prev) => ({
            ...prev,
            isListening: true,
            isSimulatedListening: isSim,
            instructionText: 'Listening…',
          }));
        },
        onListeningEnd: () => {
          setState((prev) => ({ ...prev, isListening: false }));
        },
        onResult: (text, isSim) => {
          executeCommand(text, isSim);
        },
        onError: (errMsg, isSim) => {
          setState((prev) => ({
            ...prev,
            isListening: false,
            isSimulatedListening: isSim,
            instructionText: errMsg,
          }));
        },
      },
      state.session.isActive
    );
  };

  const executeCommand = async (rawText: string, isSimulated: boolean) => {
    const command = parseVoiceCommand(rawText);

    setState((prev) => ({
      ...prev,
      youSaidText: rawText,
      isSimulatedListening: isSimulated,
    }));

    switch (command.type) {
      case 'NAVIGATE_TO': {
        setState((prev) => ({
          ...prev,
          isRouting: true,
          instructionText: `Finding route to ${command.destination}…`,
        }));
        const { announcement, session } = await navEngine.startRoute(
          command.destination,
          state.currentLocation
        );
        setState((prev) => ({
          ...prev,
          isRouting: false,
          instructionText: announcement,
          session,
        }));
        break;
      }
      case 'FIND_NEAREST': {
        const dest = `${command.poiType} near me`;
        setState((prev) => ({
          ...prev,
          isRouting: true,
          instructionText: `Searching for nearest ${command.poiType}…`,
        }));
        const { announcement, session } = await navEngine.startRoute(
          dest,
          state.currentLocation
        );
        setState((prev) => ({
          ...prev,
          isRouting: false,
          instructionText: announcement,
          session,
        }));
        break;
      }
      case 'HOW_FAR': {
        const answer = navEngine.howFar();
        setState((prev) => ({ ...prev, instructionText: answer }));
        break;
      }
      case 'NEXT_TURN': {
        const answer = navEngine.nextTurn();
        setState((prev) => ({ ...prev, instructionText: answer }));
        break;
      }
      case 'REPEAT': {
        const answer = navEngine.repeat();
        setState((prev) => ({ ...prev, instructionText: answer }));
        break;
      }
      case 'REROUTE': {
        if (state.session.destinationName) {
          setState((prev) => ({
            ...prev,
            isRouting: true,
            instructionText: `Rerouting to ${state.session.destinationName}…`,
          }));
          const { announcement, session } = await navEngine.startRoute(
            state.session.destinationName,
            state.currentLocation
          );
          setState((prev) => ({
            ...prev,
            isRouting: false,
            instructionText: announcement,
            session,
          }));
        } else {
          const msg = 'No active route to recalculate.';
          speechService.speak(msg);
          setState((prev) => ({ ...prev, instructionText: msg }));
        }
        break;
      }
      case 'STOP_NAVIGATION': {
        const msg = navEngine.stop();
        setState((prev) => ({
          ...prev,
          instructionText: msg,
          session: navEngine.getSession(),
        }));
        break;
      }
      case 'HELP': {
        const help =
          'Commands: Take me to [place], Nearest petrol pump, How far, Next turn, Repeat, Stop navigation.';
        speechService.speak(help);
        setState((prev) => ({ ...prev, instructionText: help }));
        break;
      }
      case 'UNKNOWN': {
        const unk = `Command not recognized: "${rawText}".`;
        speechService.speak(unk);
        setState((prev) => ({ ...prev, instructionText: unk }));
        break;
      }
    }
  };

  const handleSaveApiKey = async (key: string) => {
    await orsService.saveApiKey(key);
    setState((prev) => ({
      ...prev,
      apiKey: key,
      isApiKeyConfigured: !!key.trim(),
      isSettingsOpen: false,
    }));
  };

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="light-content" backgroundColor={COLORS.background} />

      {/* Top Header */}
      <View style={styles.header}>
        <Text style={styles.brandTitle}>VOICE NAV</Text>
        <TouchableOpacity
          style={styles.settingsBtn}
          onPress={() =>
            setState((prev) => ({ ...prev, isSettingsOpen: !prev.isSettingsOpen }))
          }
          accessibilityLabel="Settings"
          accessibilityRole="button"
        >
          <Text
            style={[
              styles.settingsIcon,
              { color: state.isSettingsOpen ? COLORS.amberAccent : COLORS.mutedText },
            ]}
          >
            ⚙
          </Text>
        </TouchableOpacity>
      </View>

      {/* Inline Settings Card */}
      {state.isSettingsOpen && (
        <InlineSettings
          apiKey={state.apiKey}
          isConfigured={state.isApiKeyConfigured}
          onSave={handleSaveApiKey}
          onClose={() => setState((prev) => ({ ...prev, isSettingsOpen: false }))}
        />
      )}

      {/* Main Center Area */}
      <View style={styles.centerArea}>
        {state.isSimulatedListening && (
          <View style={styles.badge}>
            <Text style={styles.badgeText}>SIMULATED VOICE INPUT</Text>
          </View>
        )}

        {/* Large instruction text area (~30px, high contrast) */}
        <Text style={styles.instructionText}>{state.instructionText}</Text>

        {/* Smaller "You said: ..." line */}
        <Text style={styles.youSaidText}>
          {state.youSaidText ? `You said: "${state.youSaidText}"` : 'Tap Ask and speak your destination'}
        </Text>

        {state.isRouting && (
          <ActivityIndicator
            color={COLORS.amberAccent}
            size="small"
            style={{ marginTop: 16 }}
          />
        )}
      </View>

      {/* Bottom Area: Large Microphone-Style Button */}
      <View style={styles.bottomArea}>
        <MicrophoneButton
          isListening={state.isListening}
          onPress={handleAskPress}
        />

        {/* Quick evaluation pills for rider commands */}
        <View style={styles.quickChipsRow}>
          <TouchableOpacity
            style={styles.chip}
            onPress={() => executeCommand('Take me to Shaniwar Wada', true)}
          >
            <Text style={styles.chipText}>Shaniwar Wada</Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={styles.chip}
            onPress={() => executeCommand('Nearest petrol pump', true)}
          >
            <Text style={styles.chipText}>Petrol Pump</Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={styles.chip}
            onPress={() => executeCommand('How far?', true)}
          >
            <Text style={styles.chipText}>How Far?</Text>
          </TouchableOpacity>
        </View>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: COLORS.background,
    paddingHorizontal: 24,
    paddingVertical: 12,
    justifyContent: 'space-between',
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    height: 48,
  },
  brandTitle: {
    color: COLORS.amberAccent,
    fontSize: 14,
    fontWeight: '700',
    letterSpacing: 2,
    fontFamily: 'monospace',
  },
  settingsBtn: {
    width: 48,
    height: 48,
    justifyContent: 'center',
    alignItems: 'center',
  },
  settingsIcon: {
    fontSize: 22,
  },
  centerArea: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingVertical: 20,
  },
  badge: {
    backgroundColor: COLORS.surfaceVariant,
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 4,
    marginBottom: 20,
  },
  badgeText: {
    color: COLORS.amberAccent,
    fontSize: 11,
    fontWeight: '700',
    letterSpacing: 1,
  },
  instructionText: {
    fontSize: 30,
    fontWeight: '700',
    color: COLORS.text,
    textAlign: 'center',
    lineHeight: 38,
  },
  youSaidText: {
    fontSize: 15,
    color: COLORS.amberAccent,
    marginTop: 20,
    textAlign: 'center',
  },
  bottomArea: {
    alignItems: 'center',
    paddingBottom: 24,
  },
  quickChipsRow: {
    flexDirection: 'row',
    marginTop: 20,
    gap: 8,
  },
  chip: {
    backgroundColor: COLORS.surfaceVariant,
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 16,
  },
  chipText: {
    color: COLORS.text,
    fontSize: 12,
    fontWeight: '500',
  },
});
