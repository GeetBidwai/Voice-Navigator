import * as Speech from 'expo-speech';

export class SpeechOutputService {
  async speak(text: string): Promise<void> {
    try {
      const isSpeaking = await Speech.isSpeakingAsync();
      if (isSpeaking) {
        await Speech.stop();
      }
      Speech.speak(text, {
        language: 'en-IN',
        pitch: 1.0,
        rate: 0.95,
      });
    } catch {
      // Graceful fallback for web/unsupported runtimes
      console.log(`[TTS]: ${text}`);
    }
  }

  async stop(): Promise<void> {
    try {
      await Speech.stop();
    } catch {
      // Ignored
    }
  }
}
