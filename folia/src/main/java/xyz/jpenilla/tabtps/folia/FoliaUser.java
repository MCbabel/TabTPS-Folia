/*
 * This file is part of TabTPS, licensed under the MIT License.
 *
 * Copyright (c) 2020-2024 Jason Penilla
 * Copyright (c) 2026 MCbabel (Folia support)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package xyz.jpenilla.tabtps.folia;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.tabtps.common.AbstractUser;
import xyz.jpenilla.tabtps.common.TabTPS;
import xyz.jpenilla.tabtps.common.util.Serializers;

@DefaultQualifier(NonNull.class)
public final class FoliaUser extends AbstractUser<Player> {
  private final BukkitAudiences audiences;
  private @MonotonicNonNull Audience audience;

  private FoliaUser(final TabTPS tabTPS, final Player player) {
    super(tabTPS, player, player.getUniqueId());
    this.audiences = ((TabTPSFolia) tabTPS.platform()).audiences();
  }

  public static FoliaUser from(final TabTPS tabTPS, final Player player) {
    return new FoliaUser(tabTPS, player);
  }

  @Override
  public Component displayName() {
    return Serializers.LEGACY_SECTION.deserialize(this.base().getDisplayName());
  }

  @Override
  public boolean hasPermission(final String permissionString) {
    return this.base().hasPermission(permissionString);
  }

  @Override
  public boolean online() {
    return this.base().isOnline();
  }

  @Override
  public int ping() {
    return this.base().getPing();
  }

  @Override
  public Audience audience() {
    if (this.audience == null) {
      this.audience = this.audiences.player(this.base());
    }
    return this.audience;
  }
}
