/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
import React from 'react';
import { connect } from 'react-redux';
import init from '../init';
import { getSettingValue } from '../../../store/rootReducer';

class UpdateCenterAppContainer extends React.Component {
  componentDidMount () {
    this.stop = init(this.refs.container, this.props.updateCenterActive);
  }

  componentWillUnmount () {
    this.stop();
  }

  render () {
    // placing container inside div is required,
    // because when backbone.marionette's layout is destroyed,
    // it also destroys the root element,
    // but react wants it to be there to unmount it
    return (
        <div>
          <div ref="container"/>
        </div>
    );
  }
}

const mapStateToProps = state => ({
  updateCenterActive: (getSettingValue(state, 'sonar.updatecenter.activate') || {}).value
});

export default connect(mapStateToProps)(UpdateCenterAppContainer);
