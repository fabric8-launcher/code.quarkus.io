import { FormGroup, Tooltip } from "@patternfly/react-core";
import React, { ChangeEvent, useEffect } from 'react';
import { ExtendedTextInput, InputProps, InputPropsWithValidation, optionalBool, TogglePanel, useAnalyticsEditionField } from '../../core';
import './info-picker.scss';

export interface InfoPickerValue {
  groupId?: string;
  artifactId?: string;
  version?: string;
  packageName?: string;
  buildTool?: string;
}

interface InfoPickerProps extends InputPropsWithValidation<InfoPickerValue> {
  showMoreOptions?: boolean;
  quarkusVersion: string;
}

const ARTIFACTID_PATTERN = /^[a-z][a-z0-9-._]*$/;
const GROUPID_PATTERN = /^([a-zA-Z_$][a-zA-Z\d_$]*\.)*[a-zA-Z_$][a-zA-Z\d_$]*$/;

const isValidId = (value?: string) => !!value && ARTIFACTID_PATTERN.test(value || '');
const isValidGroupId = (value?: string) => !!value && GROUPID_PATTERN.test(value);
const isValidInfo = (value: InfoPickerValue) => {
  return isValidGroupId(value.groupId)
    && isValidId(value.artifactId)
    && !!value.version
    && (!value.packageName || isValidGroupId(value.packageName))
}

const SelectBuildTool= (props: InputProps<string>) => {
  const onChangeWithDirty = useAnalyticsEditionField('buildTool', props.onChange)[1]
  const adaptedOnChange = (e: ChangeEvent<HTMLSelectElement>) => {
    onChangeWithDirty(e.target.value, e);
  };
  return (
    <FormGroup
      fieldId="buildTool"
      label="Build Tool"
      aria-label="Choose build tool">
      <select id="buildtool" value={props.value || 'MAVEN'} onChange={adaptedOnChange} className={'pf-c-form-control'}>
        <option value={"MAVEN"}>Maven</option>
        <option value={"GRADLE"}>Gradle (Preview)</option>
      </select>
    </FormGroup>
  )
}

export const InfoPicker = (props: InfoPickerProps) => {
  const { value, isValid, onChange } = props;

  const onInputChange = (value: InfoPickerValue) => {
    props.onChange(value, isValidInfo(value));
  };
  useEffect(() => {
    if (isValid !== isValidInfo(value)) {
      onChange(value, !isValid);
    }
  }, [value, isValid, onChange])

  const onGroupIdChange = (newValue: string) => onInputChange({ ...value, groupId: newValue });
  const onArtifactIdChange = (newValue: string) => onInputChange({ ...value, artifactId: newValue });
  const onVersionChange = (newValue: string) => onInputChange({ ...value, version: newValue });
  const onPackageNameChange = (newValue: string) => onInputChange({ ...value, packageName: newValue });
  const onBuildToolChange = (newValue: string) => onInputChange({ ...value, buildTool: newValue });
  const configFileName = value.buildTool === 'MAVEN' ? 'pom.xml' : 'gradle.properties';
  const packageName = value.packageName === undefined ? value.groupId : value.packageName;
  return (
    <div className={`info-picker horizontal`}>
      <div className="base-settings pf-c-form">
        <ExtendedTextInput
          label="Group"
          isRequired
          type="text"
          id="groupId"
          name="groupId"
          aria-label="Edit groupId"
          value={value.groupId || ''}
          autoComplete="off"
          onChange={onGroupIdChange}
          pattern={GROUPID_PATTERN.source}
          isValid={isValidGroupId(value.groupId)}
        />
        <ExtendedTextInput
          label="Artifact"
          isRequired
          type="text"
          id="artifactId"
          name="artifactId"
          aria-label="Edit artifactId"
          value={value.artifactId || ''}
          autoComplete="off"
          onChange={onArtifactIdChange}
          pattern={ARTIFACTID_PATTERN.source}
          isValid={isValidId(value.artifactId)}
        />
        <SelectBuildTool onChange={onBuildToolChange} value={value.buildTool || 'MAVEN'}/>
      </div>
      {optionalBool(props.showMoreOptions, true) && (
        <TogglePanel id="info-extended" mode="horizontal" openLabel="Configure more options" event={["UX", "Application Info - Configure More Options"]}>
          <div className="extended-settings pf-c-form">
            <ExtendedTextInput
              label="Version"
              isRequired
              type="text"
              id="projectVersion"
              name="projectVersion"
              aria-label="Edit project version"
              value={value.version || ''}
              autoComplete="off"
              onChange={onVersionChange}
              isValid={!!value.version}
            />
            <ExtendedTextInput
              label="Package Name"
              isRequired
              type="text"
              id="packageName"
              name="packageName"
              aria-label="Edit package name"
              value={packageName || ''}
              autoComplete="off"
              onChange={onPackageNameChange}
              pattern={GROUPID_PATTERN.source}
              isValid={isValidGroupId(packageName)}
            />

            <Tooltip position="right" content={`You may change the Quarkus Version after generation in the ${configFileName}. Just be cautious with extension compatibility.`} exitDelay={0} zIndex={200}>
              <ExtendedTextInput
                label="Quarkus Version"
                isRequired
                type="text"
                id="quarkusVersion"
                name="quarkusVersion"
                aria-label="Quarkus Version"
                value={props.quarkusVersion}
                isReadOnly={true}
                className="quarkus-version"
              />
            </Tooltip>
          </div>
        </TogglePanel>
      )}
    </div>
  );
};
