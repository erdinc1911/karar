# Model dosyaları: yedek ve lisanslar

Bu sürümdeki dosyalar, Karar Eşleştirici'nin kanal ayırma için kullandığı modellerin yedeğidir.
Orijinal kaynaklar silinirse uygulama (APK) bu kopyalarla çalıştırılabilir. Hepsi MIT lisanslıdır;
MIT yeniden dağıtıma izin verir, şartı aşağıdaki telif ve lisans bildirimlerinin korunmasıdır.

| Dosya | Uygulamadaki adı | Hak sahibi | Lisans | Kaynak |
|---|---|---|---|---|
| UVR-MDX-NET-Voc_FT.onnx | Vokal / Diğerleri | Ultimate Vocal Remover (UVR) geliştiricileri Anjok07, aufr33 | MIT | https://github.com/Anjok07/ultimatevocalremovergui · indirme: https://huggingface.co/Politrees/UVR_resources |
| mel-roformer.fp16.onnx | En yüksek kalite | Model: Kimberley Jensen (KimberleyJSN) · ONNX dönüşümü: audiojs | MIT | https://huggingface.co/KimberleyJSN/melbandroformer · https://huggingface.co/audiojs/mel-roformer |
| MelBandRoformer.ckpt | (orijinal ağırlık, yeniden dönüştürmek için) | Kimberley Jensen (KimberleyJSN) | MIT | https://huggingface.co/KimberleyJSN/melbandroformer |
| htdemucs_6s_fp16weights.onnx | 6 kanal | Model: Meta Platforms, Inc. and affiliates (Demucs) · ONNX dönüşümü: adowu | MIT | https://github.com/facebookresearch/demucs · https://huggingface.co/adowu/htdemucs-6s-onnx |
| htdemucs_6s.onnx | 6 kanal (tam hassasiyet) | aynı | MIT | aynı |

UVR ekibinin isteği: modellerini kullanan uygulamalar UVR'ye ve geliştiricilerine atıf yapmalıdır
(Karar Eşleştirici bunu Ayarlar › Lisanslar ve teşekkür ekranında yapar).

SHA-256 değerleri sürüm açıklamasındadır.

## MIT License

Copyright (c) Meta Platforms, Inc. and affiliates. (Demucs)
Copyright (c) Ultimate Vocal Remover developers, Anjok07 and aufr33 (UVR models)
Copyright (c) Kimberley Jensen (Mel-Band RoFormer vocal model)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
