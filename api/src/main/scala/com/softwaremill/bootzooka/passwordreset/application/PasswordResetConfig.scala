package com.softwaremill.bootzooka.passwordreset.application

import com.flowy.marketmaker.common.ConfigWithDefault
import com.typesafe.config.Config

trait PasswordResetConfig extends ConfigWithDefault {
  def rootConfig: Config

  lazy val resetLinkPattern =
    getString("bootzooka.reset-link-pattern", "http://localhost:8080/#/password-reset?code=%s")
}
