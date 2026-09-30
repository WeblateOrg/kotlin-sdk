/*
 * SPDX-FileCopyrightText: 2026 Aayush Gupta <https://aayush.io>
 * SPDX-License-Identifier: Apache-2.0
 */

package org.weblate.sample;

import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.weblate.sample.databinding.ActivityChildBinding;

public class ChildActivity extends AppCompatActivity {

  private WeblateApp app;
  private ActivityChildBinding binding;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    binding = ActivityChildBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());

    app = (WeblateApp) getApplication();
    binding.button.setOnClickListener(v -> {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        app.getWeblate().triggerLocalizationUpdate();
      }
    });
  }
}
