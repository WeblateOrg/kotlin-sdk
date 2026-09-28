/*
 * SPDX-FileCopyrightText: 2026 Aayush Gupta <https://aayush.io>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.weblate.sample;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.weblate.android.Weblate;
import org.weblate.sample.databinding.ActivityChildBinding;

public class ChildActivity extends AppCompatActivity {

  private Weblate weblate;
  private ActivityChildBinding binding;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    binding = ActivityChildBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());

    weblate = new Weblate(this);
    binding.button.setOnClickListener(v -> weblate.triggerLocalizationUpdate());
  }
}
