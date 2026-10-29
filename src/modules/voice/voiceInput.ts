export interface VoiceInputCallbacks {
  onListeningStart: (isSimulated: boolean) => void;
  onListeningEnd: () => void;
  onResult: (text: string, isSimulated: boolean) => void;
  onError: (error: string, isSimulated: boolean) => void;
}

export class VoiceInputService {
  private isSimulatedMode = true; // Clearly default to simulated until native Expo development build includes @react-native-voice/voice
  private activeTimeout: NodeJS.Timeout | null = null;

  constructor(useSimulation: boolean = true) {
    this.isSimulatedMode = useSimulation;
  }

  isSimulated(): boolean {
    return this.isSimulatedMode;
  }

  setSimulated(simulated: boolean) {
    this.isSimulatedMode = simulated;
  }

  startListening(callbacks: VoiceInputCallbacks, isNavActive: boolean) {
    this.stopListening();

    if (this.isSimulatedMode) {
      callbacks.onListeningStart(true);

      // Simulate listening with a brief delay
      this.activeTimeout = setTimeout(() => {
        const pool = isNavActive
          ? ['How far?', 'Next turn', 'Repeat', 'Stop navigation']
          : ['Take me to Shaniwar Wada', 'Nearest petrol pump', 'Take me to FC Road'];
        const sample = pool[Math.floor(Math.random() * pool.length)];

        callbacks.onListeningEnd();
        callbacks.onResult(sample, true);
      }, 1600);
    } else {
      // Native speech recognition requires custom dev build (@react-native-voice/voice).
      // If not present in managed Expo Go, fallback gracefully.
      callbacks.onError(
        'Native SpeechRecognizer requires Expo development build. Switched to simulated listening.',
        true
      );
      this.isSimulatedMode = true;
      this.startListening(callbacks, isNavActive);
    }
  }

  stopListening() {
    if (this.activeTimeout) {
      clearTimeout(this.activeTimeout);
      this.activeTimeout = null;
    }
  }
}
