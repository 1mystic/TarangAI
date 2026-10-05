# Privacy

Tarang has **no accounts, no ads, no analytics and no servers of its own**.

* **On-device translation (ML Kit):** text never leaves your phone. Google Play services downloads the language packs
  you choose.
* **Online engine (MyMemory):** in *Smart* mode without an installed pack, or in *Online* mode, the text being
  translated (only that text) is sent over HTTPS to `api.mymemory.translated.net`. Use *Offline* mode to prevent this.
* **Speech recognition:** handled by your phone's speech service (usually Google). With offline speech packs
  installed and *Prefer offline* on, audio is processed on the device. Otherwise the speech service may process it
  online under its own privacy policy.
* **Text-to-speech and photo text reading:** run on the device.
* **History:** stored only in the app's private storage on your phone. You can clear it at any time. Android's own
  backup may include it.
* **Permissions:** microphone (to listen) and internet (pack downloads and the optional online engine). The camera
  is opened through your camera app, so Tarang never holds camera permission.
